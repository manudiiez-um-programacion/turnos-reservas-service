package ar.edu.um.turnos.config.catalogo;

import java.time.Duration;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import ar.edu.um.turnos.config.CatalogoProperties;
import ar.edu.um.turnos.config.ProblemBodyParser;
import ar.edu.um.turnos.config.catalogo.dto.AgendaProfesionalResponse;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoClientErrorException;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoErrorCode;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoHttpException;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoServerErrorException;

/**
 * Cliente tipado hacia la agenda vigente del servicio de catalogo (#16,
 * docs/contratos/catalogo-turnos.md del repo turnos-catalogo-sync).
 *
 * <p>Autenticacion y el header de correlacion los pone
 * {@link CatalogoRestClientConfig} y {@link CatalogoAuthInterceptor} con el
 * JWT tecnico interno que emite {@code InternalJwtIssuer} (#12); esta clase
 * solo construye el request y traduce la respuesta.
 *
 * <p>Es la unica operacion que este servicio consume del catalogo por ahora
 * (§4.2 del enunciado: la informacion vigente se pide antes de cada
 * operacion, nunca se replica) y es idempotente, asi que un error de
 * infraestructura ({@link CatalogoServerErrorException}, incluido el 503
 * que el contrato documenta como transitorio) o de red se reintenta con
 * backoff exponencial hasta {@code catalogo.api.max-retries} veces. Un
 * {@link CatalogoClientErrorException} (401/403/404/409) nunca se reintenta:
 * repetir la misma consulta no cambia un error funcional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoClient {

    private static final String RUTA_AGENDA = "/api/internal/professionals/{professionalId}/agenda";

    private final RestClient catalogoRestClient;
    private final CatalogoProperties catalogoProperties;

    public AgendaProfesionalResponse obtenerAgendaVigente(Long professionalId) {
        return conReintentos(() -> ejecutar(() -> catalogoRestClient.get()
                .uri(RUTA_AGENDA, professionalId)
                .retrieve()
                .body(AgendaProfesionalResponse.class)));
    }

    private <T> T ejecutar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientResponseException ex) {
            throw traducirError(ex);
        }
    }

    /**
     * Reintenta, con backoff exponencial, ante un error de infraestructura
     * ({@link CatalogoServerErrorException}) o de red
     * ({@link ResourceAccessException} — timeout o conexion rechazada). Un
     * {@link CatalogoClientErrorException} (4xx) nunca se reintenta.
     */
    private <T> T conReintentos(Supplier<T> llamada) {
        int intento = 0;
        while (true) {
            try {
                return llamada.get();
            } catch (CatalogoServerErrorException | ResourceAccessException fallo) {
                intento++;
                if (intento > catalogoProperties.maxRetries()) {
                    log.warn("Agotados los reintentos hacia el catalogo tras {} intento(s): {}",
                            intento, fallo.getMessage());
                    throw fallo;
                }
                Duration espera = catalogoProperties.retryBackoff().multipliedBy(1L << (intento - 1));
                log.warn("Reintentando la agenda vigente del catalogo (intento {}/{}, espera {}): {}",
                        intento, catalogoProperties.maxRetries(), espera, fallo.getMessage());
                dormir(espera, fallo);
            }
        }
    }

    private void dormir(Duration espera, RuntimeException falloOriginal) {
        try {
            Thread.sleep(espera.toMillis());
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            throw falloOriginal;
        }
    }

    private CatalogoHttpException traducirError(RestClientResponseException ex) {
        CuerpoProblema cuerpo = parsearCuerpo(ex);
        String code = cuerpo != null ? cuerpo.code() : null;
        String detail = cuerpo != null ? cuerpo.detail() : null;

        if (ex.getStatusCode().is5xxServerError()) {
            return new CatalogoServerErrorException(ex.getStatusCode(), code, detail);
        }
        CatalogoClientErrorException traducido = new CatalogoClientErrorException(ex.getStatusCode(), code, detail);
        if (code != null && traducido.getErrorCode() == CatalogoErrorCode.DESCONOCIDO) {
            log.warn("El catalogo respondio un code sin mapear: '{}' (status {})", code, ex.getStatusCode());
        }
        return traducido;
    }

    private CuerpoProblema parsearCuerpo(RestClientResponseException ex) {
        return ProblemBodyParser.leer(ex.getResponseBodyAsString(), CuerpoProblema.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuerpoProblema(String detail, String code) {
    }

}

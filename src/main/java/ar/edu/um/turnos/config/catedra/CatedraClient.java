package ar.edu.um.turnos.config.catedra;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import ar.edu.um.turnos.config.CatedraProperties;
import ar.edu.um.turnos.config.ProblemBodyParser;
import ar.edu.um.turnos.config.catedra.dto.CancelacionReservaResponse;
import ar.edu.um.turnos.config.catedra.dto.CancelarReservaRequest;
import ar.edu.um.turnos.config.catedra.dto.ConfirmacionHoldResponse;
import ar.edu.um.turnos.config.catedra.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.config.catedra.dto.CrearHoldRequest;
import ar.edu.um.turnos.config.catedra.dto.HoldResponse;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasFiltro;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasResponse;
import ar.edu.um.turnos.config.catedra.dto.OcupacionesResponse;
import ar.edu.um.turnos.config.catedra.dto.ReservaResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.config.catedra.exception.CatedraErrorCode;
import ar.edu.um.turnos.config.catedra.exception.CatedraFieldError;
import ar.edu.um.turnos.config.catedra.exception.CatedraHttpException;
import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.config.observabilidad.Integracion;
import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;

/**
 * Cliente tipado hacia los cinco endpoints REST que este servicio consume
 * del servicio central de la catedra (#14, §6 y §8-§12 de la referencia).
 *
 * <p>Autenticacion, timeouts y el header de correlacion los pone
 * {@link CatedraRestClientConfig} y {@link CatedraAuthInterceptor}; esta
 * clase solo construye cada request y traduce la respuesta.
 *
 * <p><strong>Reintentos solo en operaciones idempotentes</strong> (los dos
 * GET): un error de infraestructura ({@link CatedraServerErrorException}) o
 * de red se reintenta con backoff exponencial hasta
 * {@code catedra.api.max-retries} veces, dejando un log por cada intento. Las
 * operaciones con efectos (crear hold, confirmar, cancelar) nunca se
 * reintentan a ciegas desde aca: un timeout no prueba que la operacion haya
 * fallado (§18.4), asi que repetirla sin reconciliar antes por
 * {@code holdId}/{@code reservationProcessId} podria duplicar un efecto. Esa
 * reconciliacion es responsabilidad de quien use este cliente (#36), no de
 * esta clase.
 *
 * <p>Un error funcional (4xx) se traduce a {@link CatedraClientErrorException},
 * que ya trae el {@code code} crudo convertido a un {@link CatedraErrorCode}
 * propio (#15, §13.1); uno de infraestructura (5xx) se traduce a
 * {@link CatedraServerErrorException}, que no garantiza {@code code} y por
 * eso no lo traduce. Un {@code code} que llega pero no esta en
 * {@link CatedraErrorCode} queda en {@code DESCONOCIDO} y se registra: no
 * rompe el cliente, pero avisa que la referencia cambio.
 *
 * <p>Cada llamada real (exito, reintento o fallo definitivo) se registra en
 * {@link MetricasObservabilidad} (#40): alimenta los contadores de
 * reintentos/fallos de integracion y el ultimo estado conocido que lee
 * {@code /api/health} sin generar trafico nuevo hacia la catedra. Un error
 * funcional (4xx) no cuenta como fallo de integracion: la catedra respondio,
 * solo que la operacion no era valida.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatedraClient {

    private static final String HEADER_TOTAL_COUNT = "X-Total-Count";

    private final RestClient catedraRestClient;
    private final CatedraProperties catedraProperties;
    private final MetricasObservabilidad metricas;

    public OcupacionesResponse obtenerOcupaciones(Long professionalId, LocalDate from, LocalDate to) {
        return conReintentos("obtener ocupaciones", () -> ejecutar(() -> catedraRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/appointment-occupancies")
                        .queryParam("professionalId", professionalId)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .retrieve()
                .body(OcupacionesResponse.class)));
    }

    public HoldResponse crearHold(CrearHoldRequest request) {
        return conEstado(() -> ejecutar(() -> catedraRestClient.post()
                .uri("/api/appointment-holds")
                .body(request)
                .retrieve()
                .body(HoldResponse.class)));
    }

    public ConfirmacionHoldResponse confirmarHold(String holdId, ConfirmarHoldRequest request) {
        return conEstado(() -> ejecutar(() -> catedraRestClient.post()
                .uri("/api/appointment-holds/{holdId}/confirm", holdId)
                .body(request)
                .retrieve()
                .body(ConfirmacionHoldResponse.class)));
    }

    public ListarReservasResponse listarReservas(ListarReservasFiltro filtro) {
        return conReintentos("listar reservas", () -> ejecutar(() -> {
            ResponseEntity<List<ReservaResponse>> respuesta = catedraRestClient.get()
                    .uri(uriBuilder -> construirUriListado(uriBuilder, filtro))
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<ReservaResponse>>() {
                    });
            return new ListarReservasResponse(respuesta.getBody(), extraerTotal(respuesta.getHeaders()));
        }));
    }

    public CancelacionReservaResponse cancelarReserva(String reservationProcessId, String reason) {
        return conEstado(() -> ejecutar(() -> catedraRestClient.post()
                .uri("/api/appointments/{reservationProcessId}/cancel", reservationProcessId)
                .body(new CancelarReservaRequest(reason))
                .retrieve()
                .body(CancelacionReservaResponse.class)));
    }

    private URI construirUriListado(UriBuilder uriBuilder, ListarReservasFiltro filtro) {
        var builder = uriBuilder.path("/api/appointments");
        if (filtro.status() != null) {
            builder = builder.queryParam("status", filtro.status());
        }
        if (filtro.from() != null) {
            builder = builder.queryParam("from", filtro.from());
        }
        if (filtro.to() != null) {
            builder = builder.queryParam("to", filtro.to());
        }
        if (filtro.externalPatientId() != null) {
            builder = builder.queryParam("externalPatientId", filtro.externalPatientId());
        }
        if (filtro.page() != null) {
            builder = builder.queryParam("page", filtro.page());
        }
        if (filtro.size() != null) {
            builder = builder.queryParam("size", filtro.size());
        }
        if (filtro.sort() != null) {
            builder = builder.queryParam("sort", filtro.sort());
        }
        return builder.build();
    }

    private long extraerTotal(HttpHeaders headers) {
        String valor = headers.getFirst(HEADER_TOTAL_COUNT);
        if (valor == null) {
            return 0;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** Ejecuta una llamada y traduce un error HTTP no-2xx a {@link CatedraHttpException}. */
    private <T> T ejecutar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientResponseException ex) {
            throw traducirError(ex);
        }
    }

    /**
     * Para las operaciones con efectos que no se reintentan (#36): un fallo de
     * infraestructura o de red es un fallo de integracion real aunque esta
     * clase no lo reintente aca, asi que se registra igual en
     * {@link MetricasObservabilidad}. Un {@link CatedraClientErrorException}
     * (4xx) no pasa por aca: la catedra respondio, no es un fallo de
     * integracion.
     */
    private <T> T conEstado(Supplier<T> llamada) {
        try {
            T resultado = llamada.get();
            metricas.exitoIntegracion(Integracion.CATEDRA);
            return resultado;
        } catch (CatedraServerErrorException | ResourceAccessException fallo) {
            metricas.falloIntegracion(Integracion.CATEDRA);
            throw fallo;
        }
    }

    /**
     * Reintenta, con backoff exponencial, solo ante un error de
     * infraestructura ({@link CatedraServerErrorException}) o de red
     * ({@link ResourceAccessException} — timeout o conexion rechazada). Un
     * {@link CatedraClientErrorException} (4xx) nunca se reintenta: repetir
     * la misma llamada no cambia un error funcional.
     */
    private <T> T conReintentos(String operacion, Supplier<T> llamada) {
        CatedraProperties.Api configuracion = catedraProperties.api();
        int intento = 0;
        while (true) {
            try {
                T resultado = llamada.get();
                metricas.exitoIntegracion(Integracion.CATEDRA);
                return resultado;
            } catch (CatedraServerErrorException | ResourceAccessException fallo) {
                intento++;
                if (intento > configuracion.maxRetries()) {
                    metricas.falloIntegracion(Integracion.CATEDRA);
                    log.warn("Agotados los reintentos hacia la catedra en '{}' tras {} intento(s): {}",
                            operacion, intento, fallo.getMessage());
                    throw fallo;
                }
                metricas.reintentoIntegracion(Integracion.CATEDRA);
                Duration espera = configuracion.retryBackoff().multipliedBy(1L << (intento - 1));
                log.warn("Reintentando '{}' hacia la catedra (intento {}/{}, espera {}): {}",
                        operacion, intento, configuracion.maxRetries(), espera, fallo.getMessage());
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

    private CatedraHttpException traducirError(RestClientResponseException ex) {
        CuerpoProblema cuerpo = parsearCuerpo(ex);
        String code = cuerpo != null ? cuerpo.code() : null;
        String detail = cuerpo != null ? cuerpo.detail() : null;
        List<CatedraFieldError> fieldErrors = cuerpo != null ? cuerpo.fieldErrorsOVacio() : List.of();

        if (ex.getStatusCode().is5xxServerError()) {
            return new CatedraServerErrorException(ex.getStatusCode(), code, detail, fieldErrors);
        }
        CatedraClientErrorException traducido = new CatedraClientErrorException(ex.getStatusCode(), code, detail, fieldErrors);
        if (code != null && traducido.getErrorCode() == CatedraErrorCode.DESCONOCIDO) {
            log.warn("La catedra respondio un code sin mapear: '{}' (status {})", code, ex.getStatusCode());
        }
        return traducido;
    }

    private CuerpoProblema parsearCuerpo(RestClientResponseException ex) {
        return ProblemBodyParser.leer(ex.getResponseBodyAsString(), CuerpoProblema.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuerpoProblema(String detail, String code, List<CatedraFieldError> fieldErrors) {

        List<CatedraFieldError> fieldErrorsOVacio() {
            return fieldErrors == null ? List.of() : fieldErrors;
        }
    }

}

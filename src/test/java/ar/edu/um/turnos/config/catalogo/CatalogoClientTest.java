package ar.edu.um.turnos.config.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import ar.edu.um.turnos.config.CatalogoProperties;
import ar.edu.um.turnos.config.JwtProperties;
import ar.edu.um.turnos.config.catalogo.dto.AgendaProfesionalResponse;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoClientErrorException;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoErrorCode;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoNoDisponibleException;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoServerErrorException;
import ar.edu.um.turnos.config.observabilidad.EstadoIntegracion;
import ar.edu.um.turnos.config.observabilidad.Integracion;
import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;

/**
 * Cubre {@code obtenerAgendaVigente} de {@link CatalogoClient} contra un
 * servidor HTTP falso que replica los ejemplos de
 * docs/contratos/catalogo-turnos.md del repo turnos-catalogo-sync, y el
 * comportamiento de reintentos y traduccion de errores de #16: la agenda es
 * la unica operacion, es un GET idempotente, asi que se reintenta ante un
 * 503 o un error de red; un 4xx funcional nunca se reintenta.
 */
@DisplayName("CatalogoClient")
class CatalogoClientTest {

    private HttpServer servidorFalso;
    private final AtomicInteger llamadasRecibidas = new AtomicInteger();

    @BeforeEach
    void levantarServidorFalso() throws Exception {
        servidorFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidorFalso.start();
    }

    @AfterEach
    void apagarServidorFalso() {
        servidorFalso.stop(0);
    }

    private final MetricasObservabilidad metricas = new MetricasObservabilidad(new SimpleMeterRegistry());

    private CatalogoClient clienteCon(int maxRetries) {
        CatalogoProperties catalogoProperties = new CatalogoProperties(
                "http://localhost:" + servidorFalso.getAddress().getPort(),
                Duration.ofSeconds(2), Duration.ofSeconds(2), maxRetries, Duration.ofMillis(5));

        JwtProperties jwtProperties = new JwtProperties(
                "turnos-reservas-service-test", "secreto-de-usuario-no-se-usa-aca-000000000000000",
                "secreto-interno-de-prueba-de-al-menos-32-char", Duration.ofHours(1), Duration.ofMinutes(2));
        CatalogoAuthInterceptor interceptor = new CatalogoAuthInterceptor(new InternalJwtIssuer(jwtProperties));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(catalogoProperties.connectTimeout());
        requestFactory.setReadTimeout(catalogoProperties.readTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(catalogoProperties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(interceptor)
                .build();

        return new CatalogoClient(restClient, catalogoProperties, metricas);
    }

    @Test
    @DisplayName("obtenerAgendaVigente parsea la respuesta de ejemplo del contrato (#4)")
    void obtenerAgendaVigente() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> responder(exchange, 200, """
                {
                  "professional": {"id": 42, "categoryId": 7, "firstName": "Ana", "lastName": "Gomez", "enabled": true},
                  "schedules": [
                    {"id": 315, "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "13:00", "slotDurationMinutes": 30}
                  ],
                  "catalogVersion": {"version": 128, "appliedAt": "2026-10-27T18:30:00Z", "source": "SNAPSHOT"}
                }
                """));

        AgendaProfesionalResponse respuesta = clienteCon(0).obtenerAgendaVigente(42L);

        assertThat(respuesta.professional().id()).isEqualTo(42L);
        assertThat(respuesta.professional().enabled()).isTrue();
        assertThat(respuesta.schedules()).hasSize(1);
        assertThat(respuesta.schedules().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(respuesta.catalogVersion().version()).isEqualTo(128L);
        assertThat(respuesta.catalogVersion().source()).isEqualTo("SNAPSHOT");
    }

    @Test
    @DisplayName("un profesional con la agenda vacia es un 200 con schedules: [] y no un error")
    void agendaVaciaNoEsError() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> responder(exchange, 200, """
                {
                  "professional": {"id": 42, "categoryId": 7, "firstName": "Ana", "lastName": "Gomez", "enabled": true},
                  "schedules": [],
                  "catalogVersion": {"version": 128, "appliedAt": "2026-10-27T18:30:00Z", "source": "SNAPSHOT"}
                }
                """));

        AgendaProfesionalResponse respuesta = clienteCon(0).obtenerAgendaVigente(42L);

        assertThat(respuesta.schedules()).isEmpty();
    }

    @Test
    @DisplayName("un profesional inexistente (404 PROFESSIONAL_NOT_FOUND) se traduce y no se reintenta")
    void profesionalInexistenteNoSeReintenta() {
        servidorFalso.createContext("/api/internal/professionals/777/agenda", exchange -> {
            llamadasRecibidas.incrementAndGet();
            responder(exchange, 404, """
                    {"status": 404, "detail": "No hay un profesional con id 777", "code": "PROFESSIONAL_NOT_FOUND"}
                    """);
        });

        assertThatThrownBy(() -> clienteCon(3).obtenerAgendaVigente(777L))
                .isInstanceOf(CatalogoClientErrorException.class)
                .satisfies(ex -> {
                    CatalogoClientErrorException error = (CatalogoClientErrorException) ex;
                    assertThat(error.getCode()).isEqualTo("PROFESSIONAL_NOT_FOUND");
                    assertThat(error.getErrorCode()).isEqualTo(CatalogoErrorCode.PROFESSIONAL_NOT_FOUND);
                });
        assertThat(llamadasRecibidas.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("un profesional deshabilitado (409 PROFESSIONAL_DISABLED) se distingue del inexistente")
    void profesionalDeshabilitadoSeDistingueDelInexistente() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> responder(exchange, 409, """
                {"status": 409, "detail": "El profesional esta deshabilitado", "code": "PROFESSIONAL_DISABLED"}
                """));

        assertThatThrownBy(() -> clienteCon(0).obtenerAgendaVigente(42L))
                .isInstanceOf(CatalogoClientErrorException.class)
                .satisfies(ex -> assertThat(((CatalogoClientErrorException) ex).getErrorCode())
                        .isEqualTo(CatalogoErrorCode.PROFESSIONAL_DISABLED));
    }

    @Test
    @DisplayName("un 401 sin cuerpo (rechazo directo de Spring Security) no tiene code ni detail, y su errorCode es DESCONOCIDO")
    void errorSinCuerpo() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> clienteCon(0).obtenerAgendaVigente(42L))
                .isInstanceOf(CatalogoClientErrorException.class)
                .satisfies(ex -> {
                    CatalogoClientErrorException error = (CatalogoClientErrorException) ex;
                    assertThat(error.getCode()).isNull();
                    assertThat(error.getDetail()).isNull();
                    assertThat(error.getErrorCode()).isEqualTo(CatalogoErrorCode.DESCONOCIDO);
                });
    }

    @Test
    @DisplayName("un code que el catalogo todavia no documenta se mapea a DESCONOCIDO sin romper el cliente")
    void codeDesconocidoNoRompeElCliente() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> responder(exchange, 409, """
                {"status": 409, "detail": "algo nuevo", "code": "CODE_QUE_NO_EXISTE_TODAVIA"}
                """));

        assertThatThrownBy(() -> clienteCon(0).obtenerAgendaVigente(42L))
                .isInstanceOf(CatalogoClientErrorException.class)
                .satisfies(ex -> {
                    CatalogoClientErrorException error = (CatalogoClientErrorException) ex;
                    assertThat(error.getCode()).isEqualTo("CODE_QUE_NO_EXISTE_TODAVIA");
                    assertThat(error.getErrorCode()).isEqualTo(CatalogoErrorCode.DESCONOCIDO);
                });
    }

    @Test
    @DisplayName("un 503 CATALOG_NOT_SYNCHRONIZED se reintenta hasta que el catalogo termina de sincronizar")
    void seReintentaUn503HastaQueSincroniza() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> {
            int numero = llamadasRecibidas.incrementAndGet();
            if (numero <= 2) {
                responder(exchange, 503, """
                        {"status": 503, "detail": "Sin catalogo aplicado", "code": "CATALOG_NOT_SYNCHRONIZED"}
                        """);
            } else {
                responder(exchange, 200, """
                        {
                          "professional": {"id": 42, "categoryId": 7, "firstName": "Ana", "lastName": "Gomez", "enabled": true},
                          "schedules": [],
                          "catalogVersion": {"version": 128, "appliedAt": "2026-10-27T18:30:00Z", "source": "SNAPSHOT"}
                        }
                        """);
            }
        });

        AgendaProfesionalResponse respuesta = clienteCon(3).obtenerAgendaVigente(42L);

        assertThat(respuesta.schedules()).isEmpty();
        assertThat(llamadasRecibidas.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("agotados los reintentos, se envuelve en CatalogoNoDisponibleException (#17) sin perder el 503 original")
    void agotaLosReintentosYEnvuelveElNoSincronizado() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> {
            llamadasRecibidas.incrementAndGet();
            responder(exchange, 503, "{}");
        });

        assertThatThrownBy(() -> clienteCon(2).obtenerAgendaVigente(42L))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasCauseInstanceOf(CatalogoServerErrorException.class)
                .satisfies(ex -> assertThat(((CatalogoServerErrorException) ex.getCause()).esNoSincronizado()).isTrue());
        // El primer intento + 2 reintentos configurados.
        assertThat(llamadasRecibidas.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("una conexion rechazada (catalogo caido) agota los reintentos y tambien se envuelve (#17)")
    void conexionRechazadaAgotaLosReintentosYSeEnvuelve() {
        CatalogoClient cliente = clienteCon(2);
        servidorFalso.stop(0);

        assertThatThrownBy(() -> cliente.obtenerAgendaVigente(42L))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("#40: una consulta exitosa marca el catalogo como disponible en MetricasObservabilidad")
    void unaConsultaExitosaMarcaElCatalogoComoDisponible() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> responder(exchange, 200, """
                {
                  "professional": {"id": 42, "categoryId": 7, "firstName": "Ana", "lastName": "Gomez", "enabled": true},
                  "schedules": [],
                  "catalogVersion": {"version": 128, "appliedAt": "2026-10-27T18:30:00Z", "source": "SNAPSHOT"}
                }
                """));

        clienteCon(0).obtenerAgendaVigente(42L);

        EstadoIntegracion estado = metricas.estadoDe(Integracion.CATALOGO);
        assertThat(estado.esConocido()).isTrue();
        assertThat(estado.disponible()).isTrue();
        assertThat(metricas.estadoDe(Integracion.CATEDRA).esConocido())
                .as("no se toca el estado de la catedra, es una integracion distinta")
                .isFalse();
    }

    @Test
    @DisplayName("#40: agotados los reintentos, el catalogo queda marcado como no disponible")
    void agotadosLosReintentosElCatalogoQuedaNoDisponible() {
        servidorFalso.createContext("/api/internal/professionals/42/agenda", exchange -> {
            llamadasRecibidas.incrementAndGet();
            responder(exchange, 503, "{}");
        });

        assertThatThrownBy(() -> clienteCon(2).obtenerAgendaVigente(42L))
                .isInstanceOf(CatalogoNoDisponibleException.class);

        assertThat(metricas.estadoDe(Integracion.CATALOGO).disponible()).isFalse();
    }

    private static void responder(com.sun.net.httpserver.HttpExchange exchange, int status, String cuerpoJson) throws java.io.IOException {
        byte[] bytes = cuerpoJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

}

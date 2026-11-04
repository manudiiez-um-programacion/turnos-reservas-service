package ar.edu.um.turnos.config.catedra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import ar.edu.um.turnos.config.CatedraProperties;
import ar.edu.um.turnos.config.catedra.dto.CancelacionReservaResponse;
import ar.edu.um.turnos.config.catedra.dto.ConfirmacionHoldResponse;
import ar.edu.um.turnos.config.catedra.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.config.catedra.dto.CrearHoldRequest;
import ar.edu.um.turnos.config.catedra.dto.HoldResponse;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasFiltro;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasResponse;
import ar.edu.um.turnos.config.catedra.dto.OcupacionesResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;

/**
 * Cubre los cinco metodos tipados de {@link CatedraClient} contra un servidor
 * HTTP falso que replica los ejemplos de §8-§12 de la referencia, y el
 * comportamiento de reintentos y traduccion de errores de #14: solo se
 * reintentan las operaciones idempotentes (GET), con backoff y limite, y un
 * 4xx nunca se reintenta.
 */
@DisplayName("CatedraClient")
class CatedraClientTest {

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

    private CatedraClient clienteCon(int maxRetries) {
        CatedraProperties.Api api = new CatedraProperties.Api(
                "http://localhost:" + servidorFalso.getAddress().getPort(),
                Duration.ofSeconds(2), Duration.ofSeconds(2), maxRetries, Duration.ofMillis(5));
        CatedraProperties catedraProperties = new CatedraProperties(
                "grupo-de-prueba", "jwt-tecnico-de-prueba-no-es-una-credencial", api,
                new CatedraProperties.Kafka("kafka.invalido:9092", "grupo", "topic.entrada", "topic.salida"), null);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(api.connectTimeout());
        requestFactory.setReadTimeout(api.readTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(api.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(new CatedraAuthInterceptor(catedraProperties))
                .build();

        return new CatedraClient(restClient, catedraProperties);
    }

    @Test
    @DisplayName("obtenerOcupaciones parsea la respuesta de ejemplo de la referencia (§8)")
    void obtenerOcupaciones() {
        servidorFalso.createContext("/api/appointment-occupancies", exchange -> {
            responder(exchange, 200, """
                    {
                      "professionalId": 15,
                      "from": "2026-09-14",
                      "to": "2026-09-14",
                      "occupancies": [
                        {"date": "2026-09-14", "startTime": "09:00:00", "status": "CONFIRMED", "expiresAt": null},
                        {"date": "2026-09-14", "startTime": "09:30:00", "status": "HELD", "expiresAt": "2026-09-10T14:44:50Z"}
                      ]
                    }
                    """);
        });

        OcupacionesResponse respuesta = clienteCon(0).obtenerOcupaciones(15L, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14));

        assertThat(respuesta.professionalId()).isEqualTo(15L);
        assertThat(respuesta.occupancies()).hasSize(2);
        assertThat(respuesta.occupancies().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(respuesta.occupancies().get(1).status()).isEqualTo("HELD");
        assertThat(respuesta.occupancies().get(1).expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("crearHold parsea la respuesta 201 de ejemplo (§9)")
    void crearHold() {
        servidorFalso.createContext("/api/appointment-holds", exchange -> responder(exchange, 201, """
                {"holdId": "hold-6c0c2f41", "reservationProcessId": "process-a92b47d0", "status": "HELD", "expiresAt": "2026-09-10T14:44:50Z"}
                """));

        HoldResponse respuesta = clienteCon(0).crearHold(
                new CrearHoldRequest(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), "user-42"));

        assertThat(respuesta.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(respuesta.reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(respuesta.status()).isEqualTo("HELD");
    }

    @Test
    @DisplayName("confirmarHold parsea la respuesta 202 de ejemplo (§10)")
    void confirmarHold() {
        servidorFalso.createContext("/api/appointment-holds/hold-6c0c2f41/confirm", exchange -> responder(exchange, 202, """
                {"reservationProcessId": "process-a92b47d0", "status": "WAITING_FOR_PHONE", "expiresAt": "2026-09-10T14:44:50Z"}
                """));

        ConfirmacionHoldResponse respuesta = clienteCon(0).confirmarHold(
                "hold-6c0c2f41", new ConfirmarHoldRequest("user-42", "Juan", "Perez"));

        assertThat(respuesta.status()).isEqualTo("WAITING_FOR_PHONE");
    }

    @Test
    @DisplayName("listarReservas combina el body con el X-Total-Count del header (§11)")
    void listarReservas() {
        servidorFalso.createContext("/api/appointments", exchange -> {
            exchange.getResponseHeaders().add("X-Total-Count", "37");
            responder(exchange, 200, """
                    [{
                      "reservationId": 8251, "reservationProcessId": "process-a92b47d0",
                      "externalPatientId": "user-42", "appointmentDate": "2026-09-14",
                      "startTime": "09:30:00", "endTime": "10:00:00", "professionalId": 15,
                      "professionalFirstName": "Ana", "professionalLastName": "Gomez",
                      "patientFirstName": "Juan", "patientLastName": "Perez",
                      "patientPhone": "+5492615551234", "status": "CONFIRMED",
                      "createdAt": "2026-09-10T14:35:00Z", "confirmedAt": "2026-09-10T14:36:00Z",
                      "cancelledAt": null, "cancellationReason": null
                    }]
                    """);
        });

        ListarReservasResponse respuesta = clienteCon(0).listarReservas(
                new ListarReservasFiltro("CONFIRMED", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "user-42", 0, 20, null));

        assertThat(respuesta.totalCount()).isEqualTo(37L);
        assertThat(respuesta.reservas()).hasSize(1);
        assertThat(respuesta.reservas().get(0).reservationId()).isEqualTo(8251L);
    }

    @Test
    @DisplayName("cancelarReserva parsea la respuesta 200 de ejemplo (§12)")
    void cancelarReserva() {
        servidorFalso.createContext("/api/appointments/process-a92b47d0/cancel", exchange -> responder(exchange, 200, """
                {"reservationId": 8251, "reservationProcessId": "process-a92b47d0", "status": "CANCELLED", "cancelledAt": "2026-09-11T15:10:00Z"}
                """));

        CancelacionReservaResponse respuesta = clienteCon(0).cancelarReserva("process-a92b47d0", "Cambio solicitado por el usuario");

        assertThat(respuesta.status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("un 4xx funcional (ej. SLOT_ALREADY_HELD) se traduce y no se reintenta")
    void errorFuncionalNoSeReintenta() {
        servidorFalso.createContext("/api/appointment-holds", exchange -> {
            llamadasRecibidas.incrementAndGet();
            responder(exchange, 409, """
                    {"status": 409, "detail": "Slot already held", "code": "SLOT_ALREADY_HELD"}
                    """);
        });

        assertThatThrownBy(() -> clienteCon(3).crearHold(
                        new CrearHoldRequest(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), "user-42")))
                .isInstanceOf(CatedraClientErrorException.class)
                .satisfies(ex -> {
                    CatedraClientErrorException error = (CatedraClientErrorException) ex;
                    assertThat(error.getCode()).isEqualTo("SLOT_ALREADY_HELD");
                    assertThat(error.getDetail()).isEqualTo("Slot already held");
                });
        assertThat(llamadasRecibidas.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("un 401 sin cuerpo (rechazo directo de Spring Security) no tiene code ni detail")
    void errorSinCuerpo() {
        servidorFalso.createContext("/api/appointments", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> clienteCon(0).listarReservas(new ListarReservasFiltro(null, null, null, null, null, null, null)))
                .isInstanceOf(CatedraClientErrorException.class)
                .satisfies(ex -> {
                    CatedraClientErrorException error = (CatedraClientErrorException) ex;
                    assertThat(error.getCode()).isNull();
                    assertThat(error.getDetail()).isNull();
                });
    }

    @Test
    @DisplayName("un 503 en una operacion GET se reintenta hasta que la catedra se recupera")
    void seReintentaUnGetAnteUn503() {
        servidorFalso.createContext("/api/appointment-occupancies", exchange -> {
            int numero = llamadasRecibidas.incrementAndGet();
            if (numero <= 2) {
                responder(exchange, 503, "{}");
            } else {
                responder(exchange, 200, """
                        {"professionalId": 15, "from": "2026-09-14", "to": "2026-09-14", "occupancies": []}
                        """);
            }
        });

        OcupacionesResponse respuesta = clienteCon(3).obtenerOcupaciones(15L, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14));

        assertThat(respuesta.occupancies()).isEmpty();
        assertThat(llamadasRecibidas.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("agotados los reintentos de un GET, se distingue el 503 de un 500")
    void agotaLosReintentosYDistingueElServicioNoDisponible() {
        servidorFalso.createContext("/api/appointment-occupancies", exchange -> {
            llamadasRecibidas.incrementAndGet();
            responder(exchange, 503, "{}");
        });

        assertThatThrownBy(() -> clienteCon(2).obtenerOcupaciones(15L, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14)))
                .isInstanceOf(CatedraServerErrorException.class)
                .satisfies(ex -> assertThat(((CatedraServerErrorException) ex).esServicioNoDisponible()).isTrue());
        // El primer intento + 2 reintentos configurados.
        assertThat(llamadasRecibidas.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("una operacion con efectos (crearHold) nunca se reintenta ante un 503")
    void unaOperacionConEfectosNoSeReintentaAnteUn503() {
        servidorFalso.createContext("/api/appointment-holds", exchange -> {
            llamadasRecibidas.incrementAndGet();
            responder(exchange, 503, "{}");
        });

        assertThatThrownBy(() -> clienteCon(3).crearHold(
                        new CrearHoldRequest(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), "user-42")))
                .isInstanceOf(CatedraServerErrorException.class);
        assertThat(llamadasRecibidas.get()).isEqualTo(1);
    }

    private static void responder(com.sun.net.httpserver.HttpExchange exchange, int status, String cuerpoJson) throws java.io.IOException {
        byte[] bytes = cuerpoJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

}

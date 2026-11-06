package ar.edu.um.turnos.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.SecretKey;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.config.JwtProperties;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;

/**
 * Prueba de punta a punta del flujo de reserva completo (#32, evidencia
 * minima que exige el enunciado en §11): camino feliz, rechazo reintentable,
 * limite de reintentos, vencimiento, proceso invalido, duplicados, no
 * retroceso de estados finales, orden invertido entre REST y Kafka, y
 * aislamiento entre usuarios.
 *
 * <p>Pega al {@code HoldController} real a traves de {@code MockMvc} y usa
 * un broker Kafka embebido (sin Docker, mismo patron que {@code
 * MensajeriaKafkaIntegrationTest}, #21) para los eventos de
 * {@code alumnos.turnos.acciones.{groupId}}: todos los casos de uso de
 * {@code reservas} son los reales, sin mockear.
 *
 * <p>Lo que SI se simula: los dos servidores HTTP externos (catalogo y
 * catedra, mismo patron que {@code DisponibilidadEndToEndTest}, #25) y, con
 * dos fakes en memoria, la persistencia de {@code ProcesoReserva} y la
 * deduplicacion por {@code eventId} — las pruebas contra PostgreSQL de
 * verdad llegan con Testcontainers en #41, y este slice todavia no las
 * tiene. {@link AuthService} tambien se mockea: resuelve el usuario
 * autenticado sin necesitar la tabla {@code usuario}, que ya esta cubierta
 * aparte por {@code AuthSecurityIntegrationTest}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                ReservaEndToEndTest.ACTIONS_TOPIC,
                ReservaEndToEndTest.PHONE_TOPIC
        })
@TestPropertySource(properties = {
        "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "turnos.kafka.listener.auto-startup=true"
})
@Import(ReservaEndToEndTest.FakesConfig.class)
@DisplayName("Reserva de punta a punta: hold, confirmacion, telefono y resultado final")
class ReservaEndToEndTest {

    static final String ACTIONS_TOPIC = "alumnos.turnos.acciones.proyecto-de-prueba";
    static final String PHONE_TOPIC = "catedra.turnos.telefono.proyecto-de-prueba";

    private static final Long PROFESSIONAL_ID = 15L;
    private static final String FECHA = "2026-09-14";
    private static final String HORA_INICIO = "09:30";
    private static final String TELEFONO_VALIDO = "+5492615551234";

    private static HttpServer catalogoSimulado;
    private static HttpServer catedraSimulado;
    private static final AtomicInteger CONTADOR_HOLDS = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private FakeProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    private KafkaTemplate<String, String> productorDePrueba;
    // Estatico y no de instancia: JUnit crea una instancia de la clase por cada @Test, pero la
    // deduplicacion por eventId (FakeRegistrarEventoProcesadoPort) es un bean de Spring compartido
    // por toda la clase. Un contador por instancia generaria el mismo eventId en tests distintos
    // (p.ej. "confirmation-1" en dos pruebas) y el segundo se descartaria como duplicado.
    private static final AtomicInteger contadorEventos = new AtomicInteger();

    @BeforeAll
    static void levantarServidoresSimulados() throws IOException {
        catalogoSimulado = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        catalogoSimulado.createContext("/api/internal/professionals/", ReservaEndToEndTest::responderAgenda);
        catalogoSimulado.start();

        catedraSimulado = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        catedraSimulado.createContext("/api/appointment-holds", ReservaEndToEndTest::responderHoldsCatedra);
        catedraSimulado.start();
    }

    @AfterAll
    static void apagarServidoresSimulados() {
        catalogoSimulado.stop(0);
        catedraSimulado.stop(0);
    }

    @DynamicPropertySource
    static void apuntarALosServidoresSimulados(DynamicPropertyRegistry registro) {
        registro.add("catalogo.api.base-url", () -> "http://localhost:" + catalogoSimulado.getAddress().getPort());
        registro.add("catedra.api.base-url", () -> "http://localhost:" + catedraSimulado.getAddress().getPort());
    }

    private static void responderAgenda(HttpExchange exchange) throws IOException {
        String cuerpo = """
                {
                  "professional": {"id": 15, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez", "enabled": true},
                  "schedules": [
                    {"id": 1, "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "10:00:00", "slotDurationMinutes": 30}
                  ],
                  "catalogVersion": null
                }
                """;
        responder(exchange, 200, cuerpo);
    }

    private static void responderHoldsCatedra(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(exchange.getRequestMethod()) && path.equals("/api/appointment-holds")) {
            int numero = CONTADOR_HOLDS.incrementAndGet();
            String holdId = "hold-e2e-" + numero;
            String reservationProcessId = "process-e2e-" + numero;
            String expiresAt = Instant.now().plusSeconds(600).toString();
            responder(exchange, 201, """
                    {"holdId":"%s","reservationProcessId":"%s","status":"HELD","expiresAt":"%s"}
                    """.formatted(holdId, reservationProcessId, expiresAt));
            return;
        }
        if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/confirm")) {
            String expiresAt = Instant.now().plusSeconds(600).toString();
            responder(exchange, 202, """
                    {"reservationProcessId":"no-usado","status":"WAITING_FOR_PHONE","expiresAt":"%s"}
                    """.formatted(expiresAt));
            return;
        }
        responder(exchange, 404, "{}");
    }

    private static void responder(HttpExchange exchange, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @BeforeEach
    void prepararUsuariosYProductor() {
        // Bean singleton compartido por toda la clase (ver el comentario de contadorEventos):
        // sin esto, el guardia de #36 contra un segundo hold (buscarVigentePorSlot) encontraria
        // el proceso no final que deja la prueba anterior para el mismo profesional/fecha/hora/
        // usuario y lo devolveria en vez de dejar que crearHold() pida uno nuevo.
        procesoReservaRepositoryPort.limpiar();

        when(authService.obtenerPorLogin("usuario.propietario")).thenReturn(Usuario.builder()
                .id(7L).login("usuario.propietario").externalPatientId("ext-7").build());
        when(authService.obtenerPorLogin("usuario.ajeno")).thenReturn(Usuario.builder()
                .id(99L).login("usuario.ajeno").externalPatientId("ext-99").build());

        Map<String, Object> propiedades = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        propiedades.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propiedades.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> fabrica = new DefaultKafkaProducerFactory<>(propiedades);
        productorDePrueba = new KafkaTemplate<>(fabrica);
    }

    @AfterEach
    void apagarProductorDePrueba() {
        productorDePrueba.destroy();
    }

    @TestConfiguration
    static class FakesConfig {

        @Bean
        @Primary
        ProcesoReservaRepositoryPort procesoReservaRepositoryPortFake() {
            return new FakeProcesoReservaRepositoryPort();
        }

        @Bean
        @Primary
        RegistrarEventoProcesadoPort registrarEventoProcesadoPortFake() {
            return new FakeRegistrarEventoProcesadoPort();
        }

        /**
         * {@code ProcesarEventoAccionUseCaseImpl.procesar} es {@code @Transactional}: sin este
         * reemplazo, Spring abre la transaccion contra el {@code JpaTransactionManager} real antes
         * de entrar al metodo, lo que pide una conexion real a Hikari aunque los dos puertos de
         * arriba ya esten fakeados, y cuelga el hilo del oyente de Kafka ~30s por mensaje hasta
         * agotar el pool. No hay recurso transaccional real que coordinar con los fakes en memoria,
         * asi que alcanza con un manejador que no haga nada.
         */
        @Bean
        @Primary
        PlatformTransactionManager platformTransactionManagerFake() {
            return new NoOpTransactionManager();
        }

    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // nada que iniciar: no hay recurso transaccional real detras de los fakes.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // nada que confirmar.
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // nada que revertir.
        }

    }

    /** Fake en memoria: las pruebas con PostgreSQL de verdad llegan con Testcontainers en #41. */
    static class FakeProcesoReservaRepositoryPort implements ProcesoReservaRepositoryPort {

        private final AtomicLong secuencia = new AtomicLong();
        private final Map<String, ProcesoReserva> porReservationProcessId = new ConcurrentHashMap<>();
        private final Map<String, ProcesoReserva> porHoldId = new ConcurrentHashMap<>();

        @Override
        public ProcesoReserva guardar(ProcesoReserva proceso) {
            if (proceso.getId() == null) {
                proceso.setId(secuencia.incrementAndGet());
            }
            porReservationProcessId.put(proceso.getReservationProcessId(), proceso);
            porHoldId.put(proceso.getHoldId(), proceso);
            return proceso;
        }

        @Override
        public Optional<ProcesoReserva> buscarPorReservationProcessId(String reservationProcessId) {
            return Optional.ofNullable(porReservationProcessId.get(reservationProcessId));
        }

        @Override
        public Optional<ProcesoReserva> buscarPorHoldId(String holdId) {
            return Optional.ofNullable(porHoldId.get(holdId));
        }

        @Override
        public PaginaProcesoReserva buscarPorUsuario(FiltroReservasUsuario filtro) {
            List<ProcesoReserva> coincidentes = porHoldId.values().stream()
                    .filter(proceso -> proceso.getUsuarioId().equals(filtro.usuarioId()))
                    .filter(proceso -> filtro.estado() == null || filtro.estado() == proceso.getEstado())
                    .filter(proceso -> filtro.desde() == null || !proceso.getFecha().isBefore(filtro.desde()))
                    .filter(proceso -> filtro.hasta() == null || !proceso.getFecha().isAfter(filtro.hasta()))
                    .sorted(Comparator.comparing(ProcesoReserva::getFecha).thenComparing(ProcesoReserva::getHoraInicio)
                            .reversed())
                    .toList();
            List<ProcesoReserva> pagina = coincidentes.stream()
                    .skip((long) filtro.pagina() * filtro.tamanio())
                    .limit(filtro.tamanio())
                    .toList();
            return new PaginaProcesoReserva(pagina, coincidentes.size());
        }

        @Override
        public Optional<ProcesoReserva> buscarVigentePorSlot(
                Long usuarioId, Long professionalId, LocalDate fecha, LocalTime horaInicio, Instant ahora) {
            return porHoldId.values().stream()
                    .filter(proceso -> proceso.getUsuarioId().equals(usuarioId))
                    .filter(proceso -> proceso.getProfessionalId().equals(professionalId))
                    .filter(proceso -> proceso.getFecha().equals(fecha))
                    .filter(proceso -> proceso.getHoraInicio().equals(horaInicio))
                    .filter(proceso -> !proceso.getEstado().esFinal())
                    .filter(proceso -> proceso.getExpiresAt() == null || proceso.getExpiresAt().isAfter(ahora))
                    .findFirst();
        }

        @Override
        public List<ProcesoReserva> listarNoFinales() {
            return porHoldId.values().stream().filter(proceso -> !proceso.getEstado().esFinal()).toList();
        }

        void limpiar() {
            porReservationProcessId.clear();
            porHoldId.clear();
        }

    }

    /** Fake en memoria del mismo criterio que {@link FakeProcesoReservaRepositoryPort}. */
    static class FakeRegistrarEventoProcesadoPort implements RegistrarEventoProcesadoPort {

        private final Set<String> eventIdsVistos = ConcurrentHashMap.newKeySet();

        @Override
        public boolean intentarRegistrar(EventoAccionReserva evento) {
            return eventIdsVistos.add(evento.getEventId());
        }

    }

    // ------------------------------------------------------------------
    // Helpers de REST, JWT y Kafka
    // ------------------------------------------------------------------

    private String tokenPara(String login) {
        SecretKey clave = Keys.hmacShaKeyFor(jwtProperties.userSecret().getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(login)
                .claim("auth", List.of("ROLE_USER"))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(60)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    private String[] crearHold(String token) throws Exception {
        String cuerpo = """
                {"professionalId": %d, "date": "%s", "startTime": "%s"}
                """.formatted(PROFESSIONAL_ID, FECHA, HORA_INICIO);

        String respuesta = mockMvc.perform(post("/api/holds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String holdId = JsonPath.read(respuesta, "$.holdId");
        String reservationProcessId = JsonPath.read(respuesta, "$.reservationProcessId");
        return new String[] {holdId, reservationProcessId};
    }

    private void confirmarHold(String holdId, String token) throws Exception {
        mockMvc.perform(post("/api/holds/{holdId}/confirm", holdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientFirstName": "Juan", "patientLastName": "Perez"}
                                """))
                .andExpect(status().isAccepted());
    }

    private void enviarTelefono(String holdId, String token, String telefono) throws Exception {
        mockMvc.perform(post("/api/holds/{holdId}/phone", holdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s"}
                                """.formatted(telefono)))
                .andExpect(status().isAccepted());
    }

    private void publicarEnAcciones(String reservationProcessId, String payloadJson) throws Exception {
        productorDePrueba.send(new ProducerRecord<>(ACTIONS_TOPIC, reservationProcessId, payloadJson))
                .get(5, TimeUnit.SECONDS);
    }

    private String siguienteEventId(String prefijo) {
        return prefijo + "-" + contadorEventos.incrementAndGet();
    }

    private String estadoActual(String holdId, String token) throws Exception {
        return mockMvc.perform(get("/api/holds/{holdId}", holdId).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
    }

    /** Hace polling del GET hasta que el proceso alcance el estado esperado, o falla tras 10 segundos. */
    private String esperarEstado(String holdId, String token, String estadoEsperado) throws Exception {
        long limite = System.currentTimeMillis() + 10_000;
        String ultimoCuerpo = null;
        while (System.currentTimeMillis() < limite) {
            ultimoCuerpo = estadoActual(holdId, token);
            String estado = JsonPath.read(ultimoCuerpo, "$.status");
            if (estadoEsperado.equals(estado)) {
                return ultimoCuerpo;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("el proceso no alcanzo el estado " + estadoEsperado + "; ultimo cuerpo: " + ultimoCuerpo);
    }

    /** Sin una señal positiva de "no paso nada", se le da tiempo al consumidor y se verifica despues. */
    private void darTiempoAlConsumidor() throws InterruptedException {
        Thread.sleep(800);
    }

    /** JsonPath puede devolver Integer o Long segun el tamano del numero: se normaliza a long. */
    private long reservationIdDe(String cuerpo) {
        Number reservationId = JsonPath.read(cuerpo, "$.reservationId");
        return reservationId.longValue();
    }

    private String additionalInformationRequested(String eventId, String reservationProcessId, String holdId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "AdditionalInformationRequested",
                  "reservationProcessId": "%s",
                  "holdId": "%s",
                  "requiredField": "PHONE_NUMBER",
                  "message": "Ingrese un numero de telefono para continuar.",
                  "expiresAt": "%s",
                  "occurredAt": "%s",
                  "schemaVersion": 1
                }
                """.formatted(eventId, reservationProcessId, holdId,
                Instant.now().plusSeconds(600), Instant.now());
    }

    private String additionalInformationRejected(String eventId, String reservationProcessId, String holdId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "AdditionalInformationRejected",
                  "reservationProcessId": "%s",
                  "holdId": "%s",
                  "reason": "INVALID_PHONE_NUMBER",
                  "message": "El telefono ingresado no es valido.",
                  "expiresAt": "%s",
                  "occurredAt": "%s",
                  "schemaVersion": 1
                }
                """.formatted(eventId, reservationProcessId, holdId,
                Instant.now().plusSeconds(600), Instant.now());
    }

    private String appointmentConfirmed(String eventId, String reservationProcessId, String holdId, long reservationId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "%s",
                  "holdId": "%s",
                  "reservationId": %d,
                  "status": "CONFIRMED",
                  "occurredAt": "%s",
                  "schemaVersion": 1
                }
                """.formatted(eventId, reservationProcessId, holdId, reservationId, Instant.now());
    }

    private String appointmentProcessExpired(String eventId, String reservationProcessId, String holdId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "AppointmentProcessExpired",
                  "reservationProcessId": "%s",
                  "holdId": "%s",
                  "reason": "HOLD_EXPIRED",
                  "occurredAt": "%s",
                  "schemaVersion": 1
                }
                """.formatted(eventId, reservationProcessId, holdId, Instant.now());
    }

    private String appointmentProcessInvalid(String eventId, String reservationProcessId, String holdId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "AppointmentProcessInvalid",
                  "reservationProcessId": "%s",
                  "holdId": "%s",
                  "reason": "REQUEST_EVENT_MISMATCH",
                  "occurredAt": "%s",
                  "schemaVersion": 1
                }
                """.formatted(eventId, reservationProcessId, holdId, Instant.now());
    }

    // ------------------------------------------------------------------
    // Escenarios (#32)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("camino feliz: disponibilidad -> hold -> confirmacion -> telefono -> AppointmentConfirmed, "
            + "con el reservationId verificado")
    void caminoFelizCompletoConfirmaLaReservaConElReservationId() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        confirmarHold(holdId, token);
        publicarEnAcciones(reservationProcessId,
                additionalInformationRequested(siguienteEventId("request"), reservationProcessId, holdId));
        esperarEstado(holdId, token, "ESPERANDO_TELEFONO");

        enviarTelefono(holdId, token, TELEFONO_VALIDO);
        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed(siguienteEventId("confirmation"), reservationProcessId, holdId, 8251L));

        String cuerpoFinal = esperarEstado(holdId, token, "CONFIRMADA");
        assertThat(reservationIdDe(cuerpoFinal)).isEqualTo(8251L);
    }

    @Test
    @DisplayName("rechazo reintentable: AdditionalInformationRejected deja el proceso vigente, "
            + "el segundo envio usa un eventId nuevo y termina confirmando")
    void rechazoDeTelefonoEsReintentableYTerminaConfirmando() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        confirmarHold(holdId, token);
        esperarEstado(holdId, token, "ESPERANDO_TELEFONO");

        enviarTelefono(holdId, token, TELEFONO_VALIDO);
        publicarEnAcciones(reservationProcessId,
                additionalInformationRejected(siguienteEventId("rejection"), reservationProcessId, holdId));
        String cuerpoRechazo = esperarEstado(holdId, token, "TELEFONO_RECHAZADO");
        assertThat((String) JsonPath.read(cuerpoRechazo, "$.reason")).isEqualTo("INVALID_PHONE_NUMBER");

        enviarTelefono(holdId, token, TELEFONO_VALIDO);
        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed(siguienteEventId("confirmation"), reservationProcessId, holdId, 9001L));

        esperarEstado(holdId, token, "CONFIRMADA");
    }

    @Test
    @DisplayName("limite de reintentos: al superar el maximo de intentos de telefono, el proceso no admite mas envios")
    void superadoElLimiteDeReintentosElProcesoNoAdmiteMasEnvios() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        confirmarHold(holdId, token);
        esperarEstado(holdId, token, "ESPERANDO_TELEFONO");

        for (int intento = 1; intento <= 3; intento++) {
            enviarTelefono(holdId, token, TELEFONO_VALIDO);
            publicarEnAcciones(reservationProcessId,
                    additionalInformationRejected(siguienteEventId("rejection"), reservationProcessId, holdId));
            esperarEstado(holdId, token, "TELEFONO_RECHAZADO");
        }

        mockMvc.perform(post("/api/holds/{holdId}/phone", holdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s"}
                                """.formatted(TELEFONO_VALIDO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_RETRY_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("vencimiento: AppointmentProcessExpired cierra el proceso y corta nuevos intentos de confirmacion")
    void appointmentProcessExpiredCierraElProcesoYCortaNuevosIntentos() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        confirmarHold(holdId, token);
        esperarEstado(holdId, token, "ESPERANDO_TELEFONO");

        publicarEnAcciones(reservationProcessId,
                appointmentProcessExpired(siguienteEventId("expired"), reservationProcessId, holdId));
        String cuerpoVencido = esperarEstado(holdId, token, "VENCIDA");
        assertThat((String) JsonPath.read(cuerpoVencido, "$.reason")).isEqualTo("HOLD_EXPIRED");

        mockMvc.perform(post("/api/holds/{holdId}/phone", holdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s"}
                                """.formatted(TELEFONO_VALIDO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_INVALID_STATE"));
    }

    @Test
    @DisplayName("proceso invalido: AppointmentProcessInvalid deja el proceso marcado como tal, con su motivo")
    void appointmentProcessInvalidMarcaElProcesoComoInvalido() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        publicarEnAcciones(reservationProcessId,
                appointmentProcessInvalid(siguienteEventId("invalid"), reservationProcessId, holdId));

        String cuerpo = esperarEstado(holdId, token, "INVALIDA");
        assertThat((String) JsonPath.read(cuerpo, "$.reason")).isEqualTo("REQUEST_EVENT_MISMATCH");
    }

    @Test
    @DisplayName("duplicados: reenviar el mismo AppointmentConfirmed no cambia el estado final ni el reservationId")
    void reenviarElMismoAppointmentConfirmedNoCambiaElResultado() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        String eventIdConfirmacion = siguienteEventId("confirmation");
        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed(eventIdConfirmacion, reservationProcessId, holdId, 7777L));
        esperarEstado(holdId, token, "CONFIRMADA");

        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed(eventIdConfirmacion, reservationProcessId, holdId, 7777L));
        darTiempoAlConsumidor();

        String cuerpoFinal = estadoActual(holdId, token);
        assertThat((String) JsonPath.read(cuerpoFinal, "$.status")).isEqualTo("CONFIRMADA");
        assertThat(reservationIdDe(cuerpoFinal)).isEqualTo(7777L);
    }

    @Test
    @DisplayName("estados finales no retroceden: un AdditionalInformationRequested tardio sobre un proceso "
            + "confirmado no lo reabre")
    void unAdditionalInformationRequestedTardioNoReabreUnProcesoConfirmado() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed(siguienteEventId("confirmation"), reservationProcessId, holdId, 5555L));
        esperarEstado(holdId, token, "CONFIRMADA");

        publicarEnAcciones(reservationProcessId,
                additionalInformationRequested(siguienteEventId("request-tardio"), reservationProcessId, holdId));
        darTiempoAlConsumidor();

        String cuerpoFinal = estadoActual(holdId, token);
        assertThat((String) JsonPath.read(cuerpoFinal, "$.status")).isEqualTo("CONFIRMADA");
        assertThat(reservationIdDe(cuerpoFinal)).isEqualTo(5555L);
    }

    @Test
    @DisplayName("orden invertido: AppointmentConfirmed llega antes de que se confirme el hold por REST, "
            + "y el estado converge igual")
    void appointmentConfirmedConvergeAunqueLleguePorKafkaAntesDeLaConfirmacionRest() throws Exception {
        String token = tokenPara("usuario.propietario");
        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        // Nunca se llama a confirmarHold: el resultado llega directo sobre HOLD_CREADO.
        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed(siguienteEventId("confirmation"), reservationProcessId, holdId, 3333L));

        String cuerpo = esperarEstado(holdId, token, "CONFIRMADA");
        assertThat(reservationIdDe(cuerpo)).isEqualTo(3333L);
    }

    @Test
    @DisplayName("autorizacion: otro usuario no puede confirmar ni enviar el telefono de un proceso ajeno")
    void otroUsuarioNoPuedeConfirmarNiEnviarElTelefonoDeUnProcesoAjeno() throws Exception {
        String tokenPropietario = tokenPara("usuario.propietario");
        String tokenAjeno = tokenPara("usuario.ajeno");
        String[] ids = crearHold(tokenPropietario);
        String holdId = ids[0];

        mockMvc.perform(post("/api/holds/{holdId}/confirm", holdId)
                        .header("Authorization", "Bearer " + tokenAjeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientFirstName": "Otro", "patientLastName": "Usuario"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_OWNER_MISMATCH"));

        mockMvc.perform(post("/api/holds/{holdId}/phone", holdId)
                        .header("Authorization", "Bearer " + tokenAjeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s"}
                                """.formatted(TELEFONO_VALIDO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_OWNER_MISMATCH"));
    }

}

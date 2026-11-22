package ar.edu.um.turnos.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;

/**
 * Suite de integracion de #41: la unica que corre contra PostgreSQL y Kafka
 * reales (Testcontainers), no contra dobles. Solo entra con el perfil Maven
 * {@code e2e} (ver pom.xml): {@code ./mvnw verify} sin {@code -Pe2e} ignora
 * esta clase por completo (la convencion {@code *IT.java} no coincide con el
 * include pattern de Surefire) y no necesita Docker. Instrucciones de como
 * correrla: README.md, debajo de {@code calidad:fin}.
 *
 * <p>No necesita ningun secreto de la catedra (application-e2e.properties):
 * igual que el resto de la suite, la catedra y el catalogo se simulan con un
 * servidor HTTP en memoria (mismo patron que {@code ReservaEndToEndTest},
 * #32). Lo que SI es real aca es todo lo demas: Postgres con las migraciones
 * de Flyway aplicadas de verdad, el broker de Kafka, los dos JPA repository
 * reales (sin fakes) y el registro/login de un usuario real por REST.
 *
 * <p>No repite todos los escenarios de {@code ReservaEndToEndTest} (rechazo,
 * vencimiento, duplicados, etc.): esa logica de negocio ya esta cubierta ahi
 * contra dobles, y es la misma para cualquier backend de persistencia. Esta
 * clase prueba especificamente lo que solo falla contra infraestructura real
 * (#41): que las migraciones corran y el esquema quede validado, que la
 * restriccion unica de {@code event_id} se sostenga bajo concurrencia real, y
 * que al menos un flujo completo funcione de punta a punta con Kafka real.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Testcontainers
@DisplayName("Suite e2e (#41): PostgreSQL y Kafka reales")
class ReservaPostgresKafkaIT {

    private static final String ACTIONS_TOPIC = "alumnos.turnos.acciones.proyecto-e2e";
    private static final String PHONE_TOPIC = "catedra.turnos.telefono.proyecto-e2e";
    private static final Long PROFESSIONAL_ID = 15L;
    private static final String FECHA = "2026-09-14";
    private static final String HORA_INICIO = "09:30";
    private static final String TELEFONO_VALIDO = "+5492615551234";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    private static HttpServer catalogoSimulado;
    private static HttpServer catedraSimulado;
    private static final AtomicInteger CONTADOR_HOLDS = new AtomicInteger();
    private static final AtomicInteger CONTADOR_LOGINS = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrarEventoProcesadoPort registrarEventoProcesadoPort;

    @BeforeAll
    static void levantarServidoresSimulados() throws IOException {
        catalogoSimulado = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        catalogoSimulado.createContext("/api/internal/professionals/", ReservaPostgresKafkaIT::responderAgenda);
        catalogoSimulado.start();

        catedraSimulado = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        catedraSimulado.createContext("/api/appointment-holds", ReservaPostgresKafkaIT::responderHoldsCatedra);
        catedraSimulado.start();
    }

    @AfterAll
    static void apagarServidoresSimulados() {
        catalogoSimulado.stop(0);
        catedraSimulado.stop(0);
    }

    @DynamicPropertySource
    static void propiedadesDinamicas(DynamicPropertyRegistry registro) {
        registro.add("catalogo.api.base-url", () -> "http://localhost:" + catalogoSimulado.getAddress().getPort());
        registro.add("catedra.api.base-url", () -> "http://localhost:" + catedraSimulado.getAddress().getPort());
        registro.add("catedra.kafka.bootstrap-servers", kafka::getBootstrapServers);
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
            String expiresAt = Instant.now().plusSeconds(600).toString();
            responder(exchange, 201, """
                    {"holdId":"hold-e2e-%d","reservationProcessId":"process-e2e-%d","status":"HELD","expiresAt":"%s"}
                    """.formatted(numero, numero, expiresAt));
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

    @Test
    @DisplayName("las migraciones de Flyway corren sobre la base real y el esquema queda validado")
    void migracionesCorrenYElEsquemaQuedaValidado() {
        // El contexto ya tuvo que levantar para llegar hasta aca: con
        // ddl-auto=validate (heredado de application.properties, el perfil
        // 'e2e' no lo apaga), un esquema que no coincide con las entidades
        // JPA hace fallar el arranque, no este metodo. Esto deja explicito
        // lo que ya se probo implicitamente.
        Integer migracionesFallidas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        assertThat(migracionesFallidas).isZero();

        Integer migracionesAplicadas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(migracionesAplicadas).isGreaterThan(0);

        List<String> tablasClave = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN ('proceso_reserva', 'evento_procesado')",
                String.class);
        assertThat(tablasClave).containsExactlyInAnyOrder("proceso_reserva", "evento_procesado");
    }

    @Test
    @DisplayName("dos inserciones concurrentes del mismo eventId: solo una tiene exito, la otra se descarta sin lanzar")
    void dosInsercionesConcurrentesDelMismoEventIdSoloUnaTieneExito() throws Exception {
        String eventIdCompartido = "evento-concurrencia-" + System.nanoTime();
        EventoAccionReserva evento = EventoAccionReserva.builder()
                .eventId(eventIdCompartido)
                .tipo(TipoEventoAccion.RESERVA_CONFIRMADA)
                .reservationProcessId("process-concurrencia")
                .schemaVersion(1)
                .build();

        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primero = ejecutor.submit(() -> registrarEventoProcesadoPort.intentarRegistrar(evento));
            Future<Boolean> segundo = ejecutor.submit(() -> registrarEventoProcesadoPort.intentarRegistrar(evento));

            boolean resultadoPrimero = primero.get(10, TimeUnit.SECONDS);
            boolean resultadoSegundo = segundo.get(10, TimeUnit.SECONDS);

            // La restriccion la aplica la base real (event_id es PRIMARY KEY,
            // V3__evento_procesado.sql): no hay forma de que los dos hilos
            // vean "true" sin que Postgres este resolviendo la carrera.
            assertThat(resultadoPrimero ^ resultadoSegundo)
                    .as("exactamente una de las dos inserciones concurrentes tuvo que tener exito")
                    .isTrue();

            Integer registros = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM evento_procesado WHERE event_id = ?",
                    Integer.class, eventIdCompartido);
            assertThat(registros).isEqualTo(1);
        } finally {
            ejecutor.shutdown();
        }
    }

    @Test
    @DisplayName("flujo completo de punta a punta: hold -> confirmacion -> telefono (Kafka real) -> AppointmentConfirmed")
    void flujoDeReservaCompletoDePuntaAPuntaConPostgresYKafkaReales() throws Exception {
        String token = registrarYAutenticarUsuario();

        String[] ids = crearHold(token);
        String holdId = ids[0];
        String reservationProcessId = ids[1];

        confirmarHold(holdId, token);
        publicarEnAcciones(reservationProcessId,
                additionalInformationRequested("request-e2e-" + reservationProcessId, reservationProcessId, holdId));
        esperarEstado(holdId, token, "ESPERANDO_TELEFONO");

        enviarTelefono(holdId, token, TELEFONO_VALIDO);
        // El telefono enviado tiene que haber llegado al topic de salida por
        // un broker de Kafka real, no un mock del KafkaTemplate.
        String mensajeTelefono = consumirUnMensaje(PHONE_TOPIC);
        assertThat((String) JsonPath.read(mensajeTelefono, "$.reservationProcessId")).isEqualTo(reservationProcessId);
        assertThat((String) JsonPath.read(mensajeTelefono, "$.information.phoneNumber")).isEqualTo(TELEFONO_VALIDO);

        publicarEnAcciones(reservationProcessId,
                appointmentConfirmed("confirmation-e2e-" + reservationProcessId, reservationProcessId, holdId, 9911L));

        String cuerpoFinal = esperarEstado(holdId, token, "CONFIRMADA");
        Number reservationId = JsonPath.read(cuerpoFinal, "$.reservationId");
        assertThat(reservationId.longValue()).isEqualTo(9911L);

        // La persistencia es la real: el proceso tiene que sobrevivir a una
        // consulta directa contra la misma base, no solo a traves del controller.
        Integer procesosConfirmados = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM proceso_reserva WHERE reservation_process_id = ? AND estado = 'CONFIRMADA'",
                Integer.class, reservationProcessId);
        assertThat(procesosConfirmados).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // Helpers de REST, usuario y Kafka
    // ------------------------------------------------------------------

    private String registrarYAutenticarUsuario() throws Exception {
        int numero = CONTADOR_LOGINS.incrementAndGet();
        String login = "usuario.e2e." + numero;
        String cuerpoRegistro = """
                {"login": "%s", "password": "contrasenia-de-prueba", "email": "%s@e2e.test", "langKey": "es"}
                """.formatted(login, login);
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro))
                .andExpect(status().isCreated());

        String cuerpoLogin = """
                {"username": "%s", "password": "contrasenia-de-prueba"}
                """.formatted(login);
        String respuesta = mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id_token");
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

    private String estadoActual(String holdId, String token) throws Exception {
        return mockMvc.perform(get("/api/holds/{holdId}", holdId).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
    }

    /** Hace polling del GET hasta que el proceso alcance el estado esperado, o falla tras 15 segundos. */
    private String esperarEstado(String holdId, String token, String estadoEsperado) throws Exception {
        long limite = System.currentTimeMillis() + 15_000;
        String ultimoCuerpo = null;
        while (System.currentTimeMillis() < limite) {
            ultimoCuerpo = estadoActual(holdId, token);
            String estado = JsonPath.read(ultimoCuerpo, "$.status");
            if (estadoEsperado.equals(estado)) {
                return ultimoCuerpo;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("el proceso no alcanzo el estado " + estadoEsperado + "; ultimo cuerpo: " + ultimoCuerpo);
    }

    private void publicarEnAcciones(String reservationProcessId, String payloadJson) throws Exception {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        propiedades.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propiedades.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> fabrica = new DefaultKafkaProducerFactory<>(propiedades);
        KafkaTemplate<String, String> productor = new KafkaTemplate<>(fabrica);
        try {
            productor.send(new ProducerRecord<>(ACTIONS_TOPIC, reservationProcessId, payloadJson)).get(10, TimeUnit.SECONDS);
        } finally {
            productor.destroy();
        }
    }

    private String consumirUnMensaje(String topic) {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        propiedades.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-verificacion-" + System.nanoTime());
        propiedades.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedades.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedades.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumidor = new KafkaConsumer<>(propiedades)) {
            consumidor.subscribe(List.of(topic));
            ConsumerRecords<String, String> registros = KafkaTestUtils.getRecords(consumidor, Duration.ofSeconds(15));
            assertThat(registros.count()).as("se esperaba al menos un mensaje en " + topic).isGreaterThan(0);
            return registros.iterator().next().value();
        }
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

}

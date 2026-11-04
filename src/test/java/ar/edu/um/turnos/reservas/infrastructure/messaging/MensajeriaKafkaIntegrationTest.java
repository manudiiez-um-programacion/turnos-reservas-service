package ar.edu.um.turnos.reservas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.infrastructure.messaging.adapter.InformacionAdicionalKafkaAdapter;

/**
 * Pruebas de #21 contra un broker Kafka de prueba (embebido, sin Docker, por
 * eso corre en CI sin secretos de la catedra): verifica que el cableado de
 * produccion del consumidor (#18) y del productor (#19) — configuracion,
 * {@code @KafkaListener}, serializacion — funciona de punta a punta contra un
 * broker real, no contra un mock de {@code KafkaTemplate} o de
 * {@code ConsumerRecord} como las pruebas unitarias existentes.
 *
 * <p>Se reemplaza {@link ReservasService} por un mock: la decision de negocio
 * sobre que hacer con cada tipo de evento (incluido el descarte de un
 * {@code eventType} desconocido o de un duplicado) ya esta cubierta a nivel
 * unitario por {@code ProcesarEventoAccionUseCaseImplTest} y
 * {@code EventoProcesadoRepositoryAdapterTest} (#20); lo que estas pruebas
 * agregan es la certeza de que el mensaje llega hasta ese limite tal como lo
 * entrego el broker, sin pasar por la base de datos real (eso es #41).
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                MensajeriaKafkaIntegrationTest.ACTIONS_TOPIC,
                MensajeriaKafkaIntegrationTest.PHONE_TOPIC
        })
@TestPropertySource(properties = {
        "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "turnos.kafka.listener.auto-startup=true"
})
@DisplayName("Mensajeria Kafka (consumidor y productor) contra un broker de prueba")
class MensajeriaKafkaIntegrationTest {

    static final String ACTIONS_TOPIC = "alumnos.turnos.acciones.proyecto-de-prueba";
    static final String PHONE_TOPIC = "catedra.turnos.telefono.proyecto-de-prueba";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private InformacionAdicionalKafkaAdapter informacionAdicionalKafkaAdapter;

    @MockitoBean
    private ReservasService reservasService;

    private KafkaTemplate<String, String> productorDePrueba;

    @BeforeEach
    void levantarProductorDePrueba() {
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

    private void publicarEnAcciones(String key, String payloadJson) throws ExecutionException, InterruptedException, TimeoutException {
        productorDePrueba.send(new ProducerRecord<>(ACTIONS_TOPIC, key, payloadJson)).get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("un evento bien formado, entregado por un broker real, llega mapeado al servicio")
    void unEventoBienFormadoLlegaAlServicio() throws Exception {
        publicarEnAcciones("process-a92b47d0", """
                {
                  "eventId": "confirmation-event-7d10",
                  "eventType": "AppointmentConfirmed",
                  "groupId": "proyecto-juan-perez",
                  "reservationProcessId": "process-a92b47d0",
                  "holdId": "hold-6c0c2f41",
                  "requestEventId": "request-event-8a31",
                  "reservationId": 8251,
                  "professionalId": 15,
                  "date": "2026-09-14",
                  "startTime": "09:30",
                  "status": "CONFIRMED",
                  "occurredAt": "2026-09-10T14:36:01Z",
                  "schemaVersion": 1
                }
                """);

        verify(reservasService, timeout(10_000)).procesarEventoAccion(argThat(evento ->
                "confirmation-event-7d10".equals(evento.getEventId())
                        && evento.getTipo() == TipoEventoAccion.RESERVA_CONFIRMADA
                        && "process-a92b47d0".equals(evento.getReservationProcessId())));
    }

    @Test
    @DisplayName("un evento con campos extra tambien llega, sin que el mapeo lo rechace")
    void unEventoConCamposExtraTambienLlegaAlServicio() throws Exception {
        publicarEnAcciones("process-a92b47d0", """
                {
                  "eventId": "request-event-con-extras",
                  "eventType": "AdditionalInformationRequested",
                  "reservationProcessId": "process-a92b47d0",
                  "holdId": "hold-6c0c2f41",
                  "requiredField": "PHONE_NUMBER",
                  "message": "Ingrese un numero de telefono para continuar.",
                  "expiresAt": "2026-09-10T14:44:50Z",
                  "campoNuevoQueTodaviaNoExiste": "cualquier-valor",
                  "occurredAt": "2026-09-10T14:35:10Z",
                  "schemaVersion": 1
                }
                """);

        verify(reservasService, timeout(10_000)).procesarEventoAccion(argThat(evento ->
                "request-event-con-extras".equals(evento.getEventId())
                        && evento.getTipo() == TipoEventoAccion.SOLICITUD_INFORMACION_ADICIONAL));
    }

    @Test
    @DisplayName("un eventType desconocido no rompe el consumo: el siguiente evento se procesa igual")
    void unEventTypeDesconocidoNoRompeElConsumo() throws Exception {
        publicarEnAcciones("process-a92b47d0", """
                {
                  "eventId": "evento-desconocido-integracion",
                  "eventType": "AlgoQueLaCatedraInventeMasAdelante",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-09-10T14:35:10Z",
                  "schemaVersion": 1
                }
                """);
        publicarEnAcciones("process-a92b47d0", """
                {
                  "eventId": "confirmation-event-siguiente",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-09-10T14:36:01Z",
                  "schemaVersion": 1
                }
                """);

        verify(reservasService, timeout(10_000)).procesarEventoAccion(argThat(evento ->
                "evento-desconocido-integracion".equals(evento.getEventId())
                        && evento.getTipo() == TipoEventoAccion.DESCONOCIDO));
        verify(reservasService, timeout(10_000)).procesarEventoAccion(argThat(evento ->
                "confirmation-event-siguiente".equals(evento.getEventId())));
    }

    @Test
    @DisplayName("un evento duplicado llega dos veces al servicio: Kafka no lo deduplica solo (complementa #20)")
    void unEventoDuplicadoLlegaDosVecesAlServicio() throws Exception {
        String payload = """
                {
                  "eventId": "confirmation-event-duplicado",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-duplicado",
                  "occurredAt": "2026-09-10T14:36:01Z",
                  "schemaVersion": 1
                }
                """;

        publicarEnAcciones("process-duplicado", payload);
        publicarEnAcciones("process-duplicado", payload);

        verify(reservasService, timeout(10_000).times(2)).procesarEventoAccion(argThat(evento ->
                "confirmation-event-duplicado".equals(evento.getEventId())));
    }

    @Test
    @DisplayName("el productor publica en el topic y con la message key exactos, confirmado por un broker real")
    void elProductorPublicaConElTopicYLaKeyExactosContraElBroker() {
        informacionAdicionalKafkaAdapter.publicar(InformacionAdicionalParaEnviar.builder()
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .requestEventId("request-event-8a31")
                .telefono("+5492615551234")
                .build());

        Map<String, Object> propiedadesConsumidor = KafkaTestUtils.consumerProps(
                "verificacion-productor-telefono", "true", embeddedKafkaBroker);
        propiedadesConsumidor.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedadesConsumidor.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedadesConsumidor.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumidorDePrueba =
                new org.apache.kafka.clients.consumer.KafkaConsumer<>(propiedadesConsumidor)) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumidorDePrueba, PHONE_TOPIC);
            ConsumerRecord<String, String> registro =
                    KafkaTestUtils.getSingleRecord(consumidorDePrueba, PHONE_TOPIC, Duration.ofSeconds(10));

            assertThat(registro.topic()).isEqualTo(PHONE_TOPIC);
            assertThat(registro.key()).isEqualTo("process-a92b47d0");
            assertThat(registro.value())
                    .contains("\"eventType\":\"AdditionalInformationSubmitted\"")
                    .contains("\"schemaVersion\":1")
                    .contains("\"reservationProcessId\":\"process-a92b47d0\"")
                    .contains("\"holdId\":\"hold-6c0c2f41\"")
                    .contains("\"requestEventId\":\"request-event-8a31\"")
                    .contains("\"information\":{\"phoneNumber\":\"+5492615551234\"}");
        }
    }

}

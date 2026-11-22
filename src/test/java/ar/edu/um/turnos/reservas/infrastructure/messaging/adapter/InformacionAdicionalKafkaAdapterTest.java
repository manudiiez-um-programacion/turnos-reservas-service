package ar.edu.um.turnos.reservas.infrastructure.messaging.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import ar.edu.um.turnos.config.CatedraProperties;
import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.infrastructure.messaging.exception.PublicacionInformacionAdicionalException;
import ar.edu.um.turnos.reservas.infrastructure.messaging.mapper.InformacionAdicionalMessagingMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("InformacionAdicionalKafkaAdapter")
class InformacionAdicionalKafkaAdapterTest {

    @Mock
    private KafkaTemplate<String, String> telefonoKafkaTemplate;

    private final InformacionAdicionalMessagingMapper mapper = new InformacionAdicionalMessagingMapper();

    private InformacionAdicionalKafkaAdapter adapter;

    @BeforeEach
    void setUp() {
        CatedraProperties catedraProperties = new CatedraProperties(
                "proyecto-de-prueba", "jwt-de-prueba-no-es-una-credencial",
                new CatedraProperties.Api("http://localhost", Duration.ofSeconds(2), Duration.ofSeconds(2), 0,
                        Duration.ofMillis(1)),
                new CatedraProperties.Kafka("kafka.invalido:9092", "grupo", "topic.entrada",
                        "catedra.turnos.telefono.proyecto-de-prueba"),
                null);
        adapter = new InformacionAdicionalKafkaAdapter(telefonoKafkaTemplate, mapper, catedraProperties);
    }

    private InformacionAdicionalParaEnviar informacionDePrueba() {
        return InformacionAdicionalParaEnviar.builder()
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .requestEventId("request-event-8a31")
                .telefono("+5492615551234")
                .build();
    }

    @Test
    @DisplayName("publica con el topic y la message key correctos, y el formato exacto del evento")
    void publicaConElTopicYLaMessageKeyCorrectos() {
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(telefonoKafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        adapter.publicar(informacionDePrueba());

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(telefonoKafkaTemplate).send(topic.capture(), key.capture(), value.capture());

        assertThat(topic.getValue()).isEqualTo("catedra.turnos.telefono.proyecto-de-prueba");
        assertThat(key.getValue()).isEqualTo("process-a92b47d0");
        assertThat(value.getValue()).contains("\"eventType\":\"AdditionalInformationSubmitted\"")
                .contains("\"schemaVersion\":1")
                .contains("\"reservationProcessId\":\"process-a92b47d0\"")
                .contains("\"information\":{\"phoneNumber\":\"+5492615551234\"}");
    }

    @Test
    @DisplayName("lanza si el broker no confirma la publicacion, sin darla por exitosa")
    void lanzaSiElBrokerNoConfirmaLaPublicacion() {
        CompletableFuture<SendResult<String, String>> futuroFallido = new CompletableFuture<>();
        futuroFallido.completeExceptionally(new RuntimeException("sin conexion al broker"));
        when(telefonoKafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(futuroFallido);

        assertThatThrownBy(() -> adapter.publicar(informacionDePrueba()))
                .isInstanceOf(PublicacionInformacionAdicionalException.class);
    }

}

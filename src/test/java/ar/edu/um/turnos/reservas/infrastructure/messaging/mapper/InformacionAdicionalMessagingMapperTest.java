package ar.edu.um.turnos.reservas.infrastructure.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.infrastructure.messaging.dto.InformacionAdicionalSubmittedMensaje;

@DisplayName("InformacionAdicionalMessagingMapper")
class InformacionAdicionalMessagingMapperTest {

    private final InformacionAdicionalMessagingMapper mapper = new InformacionAdicionalMessagingMapper();

    private InformacionAdicionalParaEnviar informacionDePrueba() {
        return InformacionAdicionalParaEnviar.builder()
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .requestEventId("request-event-8a31")
                .telefono("+5492615551234")
                .build();
    }

    @Test
    @DisplayName("arma el mensaje con eventType y schemaVersion fijos y los datos del proceso")
    void armaElMensajeConLosCamposFijosYLosDatosDelProceso() {
        InformacionAdicionalSubmittedMensaje mensaje =
                mapper.aMensaje(informacionDePrueba(), "proyecto-de-prueba");

        assertThat(mensaje.eventType()).isEqualTo("AdditionalInformationSubmitted");
        assertThat(mensaje.schemaVersion()).isEqualTo(1);
        assertThat(mensaje.groupId()).isEqualTo("proyecto-de-prueba");
        assertThat(mensaje.reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(mensaje.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(mensaje.requestEventId()).isEqualTo("request-event-8a31");
        assertThat(mensaje.information().phoneNumber()).isEqualTo("+5492615551234");
        assertThat(mensaje.eventId()).isNotBlank();
        assertThat(mensaje.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("genera un eventId distinto en cada envio, incluso para el mismo proceso")
    void generaUnEventIdDistintoEnCadaEnvio() {
        InformacionAdicionalParaEnviar informacion = informacionDePrueba();

        String primerEventId = mapper.aMensaje(informacion, "proyecto-de-prueba").eventId();
        String segundoEventId = mapper.aMensaje(informacion, "proyecto-de-prueba").eventId();

        assertThat(primerEventId).isNotEqualTo(segundoEventId);
    }

    @Test
    @DisplayName("serializa el formato exacto que exige el contrato, con information anidado")
    void serializaElFormatoExactoDelContrato() {
        InformacionAdicionalSubmittedMensaje mensaje =
                mapper.aMensaje(informacionDePrueba(), "proyecto-de-prueba");

        String json = mapper.aJson(mensaje);

        assertThat(json).contains("\"eventType\":\"AdditionalInformationSubmitted\"")
                .contains("\"schemaVersion\":1")
                .contains("\"groupId\":\"proyecto-de-prueba\"")
                .contains("\"reservationProcessId\":\"process-a92b47d0\"")
                .contains("\"holdId\":\"hold-6c0c2f41\"")
                .contains("\"requestEventId\":\"request-event-8a31\"")
                .contains("\"information\":{\"phoneNumber\":\"+5492615551234\"}")
                .contains("\"eventId\":\"" + mensaje.eventId() + "\"");
    }

}

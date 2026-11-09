package ar.edu.um.turnos.reservas.infrastructure.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.infrastructure.messaging.exception.EventoAccionInvalidoException;

@DisplayName("EventoAccionMessagingMapper")
class EventoAccionMessagingMapperTest {

    private final EventoAccionMessagingMapper mapper = new EventoAccionMessagingMapper();

    @Test
    @DisplayName("mapea un evento bien formado al dominio")
    void mapeaUnEventoBienFormado() {
        String payload = """
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
                """;

        EventoAccionReserva evento = mapper.aDominio("process-a92b47d0", payload);

        assertThat(evento.getEventId()).isEqualTo("confirmation-event-7d10");
        assertThat(evento.getTipo()).isEqualTo(TipoEventoAccion.RESERVA_CONFIRMADA);
        assertThat(evento.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(evento.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(evento.getOccurredAt()).isEqualTo(Instant.parse("2026-09-10T14:36:01Z"));
        assertThat(evento.getSchemaVersion()).isEqualTo(1);
        assertThat(evento.getReservationId()).isEqualTo(8251L);
    }

    @Test
    @DisplayName("mapea reason y expiresAt de AdditionalInformationRejected")
    void mapeaReasonYExpiresAtDeInformacionAdicionalRechazada() {
        String payload = """
                {
                  "eventId": "rejection-event-7d11",
                  "eventType": "AdditionalInformationRejected",
                  "reservationProcessId": "process-a92b47d0",
                  "holdId": "hold-6c0c2f41",
                  "reason": "INVALID_PHONE_NUMBER",
                  "message": "El telefono ingresado no es valido.",
                  "expiresAt": "2026-09-10T14:44:50Z",
                  "occurredAt": "2026-09-10T14:36:01Z",
                  "schemaVersion": 1
                }
                """;

        EventoAccionReserva evento = mapper.aDominio("process-a92b47d0", payload);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoAccion.INFORMACION_ADICIONAL_RECHAZADA);
        assertThat(evento.getReason()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(evento.getMessage()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(evento.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    @DisplayName("mapea reason de AppointmentProcessExpired")
    void mapeaReasonDeProcesoVencido() {
        String payload = """
                {
                  "eventId": "process-event-7d12",
                  "eventType": "AppointmentProcessExpired",
                  "reservationProcessId": "process-a92b47d0",
                  "holdId": "hold-6c0c2f41",
                  "reason": "HOLD_EXPIRED",
                  "occurredAt": "2026-09-10T14:44:52Z",
                  "schemaVersion": 1
                }
                """;

        EventoAccionReserva evento = mapper.aDominio("process-a92b47d0", payload);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoAccion.PROCESO_VENCIDO);
        assertThat(evento.getReason()).isEqualTo("HOLD_EXPIRED");
    }

    @Test
    @DisplayName("mapea los campos propios de AdditionalInformationRequested e ignora los que todavia no conoce")
    void mapeaLosCamposDeSolicitudDeInformacionAdicionalEIgnoraLosDesconocidos() {
        String payload = """
                {
                  "eventId": "request-event-8a31",
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
                """;

        EventoAccionReserva evento = mapper.aDominio("process-a92b47d0", payload);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoAccion.SOLICITUD_INFORMACION_ADICIONAL);
        assertThat(evento.getRequiredField()).isEqualTo("PHONE_NUMBER");
        assertThat(evento.getMessage()).isEqualTo("Ingrese un numero de telefono para continuar.");
        assertThat(evento.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    @DisplayName("mapea un eventType desconocido a DESCONOCIDO en lugar de lanzar")
    void mapeaUnEventTypeDesconocido() {
        String payload = """
                {
                  "eventId": "evento-x",
                  "eventType": "UnTipoQueTodaviaNoExiste",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-09-10T14:35:10Z",
                  "schemaVersion": 1
                }
                """;

        EventoAccionReserva evento = mapper.aDominio("process-a92b47d0", payload);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoAccion.DESCONOCIDO);
    }

    @Test
    @DisplayName("rechaza un JSON invalido")
    void rechazaUnJsonInvalido() {
        assertThatThrownBy(() -> mapper.aDominio("process-a92b47d0", "esto no es json"))
                .isInstanceOf(EventoAccionInvalidoException.class);
    }

    @Test
    @DisplayName("rechaza un evento sin eventId")
    void rechazaUnEventoSinEventId() {
        String payload = """
                {
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-09-10T14:36:01Z",
                  "schemaVersion": 1
                }
                """;

        assertThatThrownBy(() -> mapper.aDominio("process-a92b47d0", payload))
                .isInstanceOf(EventoAccionInvalidoException.class);
    }

    @Test
    @DisplayName("rechaza un evento sin schemaVersion")
    void rechazaUnEventoSinSchemaVersion() {
        String payload = """
                {
                  "eventId": "confirmation-event-7d10",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-09-10T14:36:01Z"
                }
                """;

        assertThatThrownBy(() -> mapper.aDominio("process-a92b47d0", payload))
                .isInstanceOf(EventoAccionInvalidoException.class);
    }

    @Test
    @DisplayName("confia en el reservationProcessId del payload aunque no coincida con la message key")
    void confiaEnElPayloadAunqueLaKeyNoCoincida() {
        String payload = """
                {
                  "eventId": "confirmation-event-7d10",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-09-10T14:36:01Z",
                  "schemaVersion": 1
                }
                """;

        EventoAccionReserva evento = mapper.aDominio("una-key-distinta", payload);

        assertThat(evento.getReservationProcessId()).isEqualTo("process-a92b47d0");
    }

}

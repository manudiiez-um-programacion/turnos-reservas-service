package ar.edu.um.turnos.reservas.infrastructure.messaging.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.infrastructure.messaging.dto.EventoAccionMensaje;
import ar.edu.um.turnos.reservas.infrastructure.messaging.exception.EventoAccionInvalidoException;
import lombok.extern.slf4j.Slf4j;

/**
 * Deserializa el {@code value} de un mensaje de
 * {@code alumnos.turnos.acciones.{groupId}} y lo traduce a
 * {@link EventoAccionReserva} (#18).
 *
 * <p>Usa una instancia propia de Jackson 2
 * ({@code com.fasterxml.jackson.databind.ObjectMapper}) y no el
 * {@code ObjectMapper} Jackson 3 que trae el contexto de Spring Boot 4: es el
 * mismo motivo que {@code ProblemBodyParser}, Spring Kafka todavia construye
 * su soporte JSON sobre Jackson 2, que ya esta en el classpath por
 * {@code jjwt-jackson}.
 */
@Slf4j
@Component
public class EventoAccionMessagingMapper {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public EventoAccionReserva aDominio(String messageKey, String payloadJson) {
        EventoAccionMensaje mensaje = leer(payloadJson);
        validarCamposComunes(mensaje);
        avisarSiLaKeyNoCoincide(messageKey, mensaje);

        return EventoAccionReserva.builder()
                .eventId(mensaje.eventId())
                .tipo(TipoEventoAccion.desde(mensaje.eventType()))
                .reservationProcessId(mensaje.reservationProcessId())
                .holdId(mensaje.holdId())
                .occurredAt(mensaje.occurredAt())
                .schemaVersion(mensaje.schemaVersion())
                .requiredField(mensaje.requiredField())
                .message(mensaje.message())
                .expiresAt(mensaje.expiresAt())
                .reservationId(mensaje.reservationId())
                .reason(mensaje.reason())
                .build();
    }

    private EventoAccionMensaje leer(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, EventoAccionMensaje.class);
        } catch (Exception noParseable) {
            throw new EventoAccionInvalidoException("el evento no es un JSON valido", noParseable);
        }
    }

    private void validarCamposComunes(EventoAccionMensaje mensaje) {
        if (esBlanco(mensaje.eventId()) || esBlanco(mensaje.eventType())
                || esBlanco(mensaje.reservationProcessId()) || mensaje.schemaVersion() == null) {
            throw new EventoAccionInvalidoException(
                    "falta eventId, eventType, reservationProcessId o schemaVersion");
        }
    }

    /**
     * Por contrato la message key es el {@code reservationProcessId} (§15.1),
     * pero un desvio aca no es un JSON invalido: se registra y se sigue
     * confiando en el campo del payload, que es el que necesitan los casos de
     * uso que despachan cada tipo de evento (#29, #31, #35).
     */
    private void avisarSiLaKeyNoCoincide(String messageKey, EventoAccionMensaje mensaje) {
        if (messageKey != null && !messageKey.equals(mensaje.reservationProcessId())) {
            log.warn("La message key '{}' no coincide con el reservationProcessId '{}' del payload (eventId={})",
                    messageKey, mensaje.reservationProcessId(), mensaje.eventId());
        }
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

}

package ar.edu.um.turnos.reservas.infrastructure.messaging.mapper;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.infrastructure.messaging.dto.InformacionAdicionalSubmittedMensaje;
import ar.edu.um.turnos.reservas.infrastructure.messaging.exception.PublicacionInformacionAdicionalException;

/**
 * Arma y serializa {@code AdditionalInformationSubmitted} (#19, §15.5 de la
 * referencia): genera el {@code eventId} propio y el {@code occurredAt},
 * fija {@code eventType} y {@code schemaVersion} a los valores exactos que
 * exige el contrato, y deja todo lo demas a cargo de quien dispara el envio.
 *
 * <p>Usa una instancia propia de Jackson 2
 * ({@code com.fasterxml.jackson.databind.ObjectMapper}) y no el
 * {@code ObjectMapper} Jackson 3 del contexto de Spring Boot 4, por el mismo
 * motivo que {@code EventoAccionMessagingMapper}: Spring Kafka construye su
 * soporte JSON sobre Jackson 2.
 */
@Component
public class InformacionAdicionalMessagingMapper {

    private static final String EVENT_TYPE = "AdditionalInformationSubmitted";
    private static final int SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public InformacionAdicionalSubmittedMensaje aMensaje(InformacionAdicionalParaEnviar informacion,
            String groupId) {
        return new InformacionAdicionalSubmittedMensaje(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                groupId,
                informacion.getReservationProcessId(),
                informacion.getHoldId(),
                informacion.getRequestEventId(),
                new InformacionAdicionalSubmittedMensaje.Information(informacion.getTelefono()),
                Instant.now(),
                SCHEMA_VERSION);
    }

    public String aJson(InformacionAdicionalSubmittedMensaje mensaje) {
        try {
            return objectMapper.writeValueAsString(mensaje);
        } catch (Exception noSerializable) {
            // No deberia pasar nunca: el DTO es propio y sin ciclos. Si pasa,
            // es un bug y no una condicion operativa, pero igual se traduce a
            // la excepcion del puerto para que quien dispare el envio (#30)
            // tenga un solo tipo de fallo que manejar.
            throw new PublicacionInformacionAdicionalException(
                    "no se pudo serializar AdditionalInformationSubmitted", noSerializable);
        }
    }

}

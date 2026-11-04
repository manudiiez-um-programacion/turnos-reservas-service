package ar.edu.um.turnos.reservas.infrastructure.messaging.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envoltorio crudo de un mensaje de {@code alumnos.turnos.acciones.{groupId}}
 * (§15.4-§15.10 de la referencia): solo los campos comunes a las seis
 * variantes.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} es la deserializacion
 * tolerante que exige #18: los campos propios de cada variante ({@code reason},
 * {@code status}, {@code requiredField}, etc.) se ignoran aca sin romper el
 * mapeo, y un campo nuevo y compatible con v1 que la catedra agregue mas
 * adelante tampoco rompe nada (§15.1).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoAccionMensaje(

        String eventId,

        String eventType,

        String reservationProcessId,

        String holdId,

        Instant occurredAt,

        Integer schemaVersion

) {
}

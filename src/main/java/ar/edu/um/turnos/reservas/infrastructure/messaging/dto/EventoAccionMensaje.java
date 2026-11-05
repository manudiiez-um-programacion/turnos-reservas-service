package ar.edu.um.turnos.reservas.infrastructure.messaging.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envoltorio crudo de un mensaje de {@code alumnos.turnos.acciones.{groupId}}
 * (§15.4-§15.10 de la referencia): los campos comunes a las seis variantes,
 * mas los propios de {@code AdditionalInformationRequested} ({@code
 * requiredField}, {@code message}, {@code expiresAt}, §15.4) que necesita
 * #29. El resto de los campos especificos de las otras variantes ({@code
 * reason}, {@code status}, etc.) siguen sin modelarse aca: los agregan los
 * casos de uso que los necesiten (#31, #35).
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} es la deserializacion
 * tolerante que exige #18: un campo que este record no conoce se ignora sin
 * romper el mapeo, y un campo nuevo y compatible con v1 que la catedra
 * agregue mas adelante tampoco rompe nada (§15.1).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoAccionMensaje(

        String eventId,

        String eventType,

        String reservationProcessId,

        String holdId,

        Instant occurredAt,

        Integer schemaVersion,

        String requiredField,

        String message,

        Instant expiresAt

) {
}

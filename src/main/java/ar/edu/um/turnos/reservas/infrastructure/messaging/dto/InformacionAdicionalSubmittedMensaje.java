package ar.edu.um.turnos.reservas.infrastructure.messaging.dto;

import java.time.Instant;

/**
 * Forma exacta de {@code AdditionalInformationSubmitted} en
 * {@code catedra.turnos.telefono.{groupId}} (#19, §15.5 de la referencia).
 *
 * <p>A diferencia de {@code EventoAccionMensaje} (que ignora lo que no
 * conoce porque lo publica la cátedra), este DTO lo serializamos nosotros:
 * el contrato exige que viajen exactamente estos campos, nada mas y nada
 * menos, con {@code eventType} y {@code schemaVersion} fijos.
 */
public record InformacionAdicionalSubmittedMensaje(

        String eventId,

        String eventType,

        String groupId,

        String reservationProcessId,

        String holdId,

        String requestEventId,

        Information information,

        Instant occurredAt,

        int schemaVersion

) {

    public record Information(String phoneNumber) {
    }

}

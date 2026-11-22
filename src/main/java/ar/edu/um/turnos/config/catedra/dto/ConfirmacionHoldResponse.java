package ar.edu.um.turnos.config.catedra.dto;

import java.time.Instant;

/**
 * Respuesta {@code 202} de {@code POST /api/appointment-holds/{holdId}/confirm}
 * (§10). La catedra crea una reserva {@code PHONE_PENDING} y publica
 * {@code AdditionalInformationRequested}; el proceso debe completarse antes
 * de {@code expiresAt}.
 */
public record ConfirmacionHoldResponse(

        String reservationProcessId,

        String status,

        Instant expiresAt

) {
}

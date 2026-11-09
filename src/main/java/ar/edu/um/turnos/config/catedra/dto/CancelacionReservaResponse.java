package ar.edu.um.turnos.config.catedra.dto;

import java.time.Instant;

/**
 * Respuesta {@code 200} de {@code POST /api/appointments/{reservationProcessId}/cancel}
 * (§12). Cancelar una reserva ya {@code CANCELLED} es idempotente y devuelve
 * su estado actual.
 */
public record CancelacionReservaResponse(

        Long reservationId,

        String reservationProcessId,

        String status,

        Instant cancelledAt

) {
}

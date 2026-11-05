package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import java.time.Instant;

/** Respuesta {@code 201} de {@code POST /api/holds} (#27). */
public record HoldResponse(

        String holdId,

        String reservationProcessId,

        String status,

        Instant expiresAt

) {
}

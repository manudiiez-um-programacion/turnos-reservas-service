package ar.edu.um.turnos.config.catedra.dto;

import java.time.Instant;

/**
 * Respuesta {@code 201} de {@code POST /api/appointment-holds} (§9). El hold
 * es temporal: la vigencia real la determina {@code expiresAt}, que manda la
 * catedra — una copia local no extiende el TTL.
 */
public record HoldResponse(

        String holdId,

        String reservationProcessId,

        String status,

        Instant expiresAt

) {
}

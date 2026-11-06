package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un elemento de {@code GET /api/reservas} (#33). {@code professionalFirstName}
 * y {@code professionalLastName} son el dato historico congelado al momento
 * del hold, nunca una consulta al catalogo vigente (§4.2 del enunciado).
 */
public record ReservaResponse(

        String holdId,

        String reservationProcessId,

        String status,

        Long professionalId,

        String professionalFirstName,

        String professionalLastName,

        LocalDate date,

        LocalTime startTime,

        Instant expiresAt,

        Long reservationId,

        String reason,

        String reasonMessage

) {
}

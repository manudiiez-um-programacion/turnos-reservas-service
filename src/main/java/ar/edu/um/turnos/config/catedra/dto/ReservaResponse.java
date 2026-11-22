package ar.edu.um.turnos.config.catedra.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una reserva de la cuenta tecnica, tal como la devuelve {@code GET /api/appointments}
 * (§11). Son las reservas de <strong>toda</strong> la cuenta tecnica del
 * proyecto: filtrarlas por usuario final es responsabilidad de este
 * servicio, nunca de la catedra.
 */
public record ReservaResponse(

        Long reservationId,

        String reservationProcessId,

        String externalPatientId,

        LocalDate appointmentDate,

        LocalTime startTime,

        LocalTime endTime,

        Long professionalId,

        String professionalFirstName,

        String professionalLastName,

        String patientFirstName,

        String patientLastName,

        String patientPhone,

        String status,

        Instant createdAt,

        Instant confirmedAt,

        Instant cancelledAt,

        String cancellationReason

) {
}

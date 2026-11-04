package ar.edu.um.turnos.config.catedra.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Body de {@code POST /api/appointment-holds} (§9). Todos los campos son obligatorios. */
public record CrearHoldRequest(

        Long professionalId,

        LocalDate date,

        LocalTime startTime,

        String externalPatientId

) {
}

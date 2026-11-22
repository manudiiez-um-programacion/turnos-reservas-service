package ar.edu.um.turnos.config.catedra.dto;

import java.time.LocalDate;
import java.util.List;

/** Respuesta de {@code GET /api/appointment-occupancies} (§8). */
public record OcupacionesResponse(

        Long professionalId,

        LocalDate from,

        LocalDate to,

        List<Ocupacion> occupancies

) {
}

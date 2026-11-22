package ar.edu.um.turnos.disponibilidad.infrastructure.web.dto;

import java.time.LocalTime;

/** Un turno libre dentro de la respuesta de {@code GET /api/disponibilidad} (#24). */
public record SlotResponse(

        LocalTime startTime,

        LocalTime endTime

) {
}

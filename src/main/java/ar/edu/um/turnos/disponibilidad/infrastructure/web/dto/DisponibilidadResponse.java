package ar.edu.um.turnos.disponibilidad.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Respuesta de {@code GET /api/disponibilidad} (#24): para KMP. */
public record DisponibilidadResponse(

        Long professionalId,

        LocalDate date,

        List<SlotResponse> availableSlots

) {
}

package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import jakarta.validation.constraints.Size;

/** Cuerpo opcional de {@code POST /api/reservas/{reservationProcessId}/cancel} (#34, §12 de la referencia). */
public record CancelarReservaRequest(

        @Size(max = 500)
        String reason

) {
}

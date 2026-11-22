package ar.edu.um.turnos.config.catedra.dto;

/** Body de {@code POST /api/appointments/{reservationProcessId}/cancel} (§12). El body entero es opcional. */
public record CancelarReservaRequest(

        String reason

) {
}

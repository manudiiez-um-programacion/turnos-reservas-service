package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /api/holds} (#27). No lleva
 * {@code externalPatientId}: el usuario se deriva siempre del JWT
 * autenticado, nunca de un valor que mande el cliente (§4.2 del enunciado).
 */
public record CrearHoldRequest(

        @NotNull
        Long professionalId,

        @NotNull
        LocalDate date,

        @NotNull
        LocalTime startTime

) {
}

package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/holds/{holdId}/confirm} (#28, §10 de la
 * referencia). No lleva {@code externalPatientId}: el usuario se deriva
 * siempre del JWT autenticado, nunca de un valor que mande el cliente
 * (§4.2 del enunciado).
 */
public record ConfirmarHoldRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        String patientFirstName,

        @NotBlank
        @Size(min = 2, max = 100)
        String patientLastName

) {
}

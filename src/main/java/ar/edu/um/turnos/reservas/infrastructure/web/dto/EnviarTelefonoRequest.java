package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/holds/{holdId}/phone} (#30, §15.5 de la
 * referencia). {@code phoneNumber} todavia es el valor crudo, sin
 * normalizar: esa logica es del caso de uso, no de este borde web.
 */
public record EnviarTelefonoRequest(

        @NotBlank
        @Size(max = 30)
        String phoneNumber

) {
}

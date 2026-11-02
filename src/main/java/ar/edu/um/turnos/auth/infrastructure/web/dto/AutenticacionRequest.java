package ar.edu.um.turnos.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/authenticate}. Mismos limites de tamano que
 * {@code RegistroRequest} (login 1-50, password 4-100), igual que el
 * {@code LoginVM} de JHipster. Sin {@code rememberMe}: no hay refresh
 * tokens en el alcance de #10.
 */
public record AutenticacionRequest(

        @NotBlank
        @Size(min = 1, max = 50)
        String username,

        @NotBlank
        @Size(min = 4, max = 100)
        String password

) {
}

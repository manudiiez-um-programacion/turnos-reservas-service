package ar.edu.um.turnos.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/register}. Las validaciones replican las del
 * contrato que genera JHipster ({@code ManagedUserVM} / {@code User}), para
 * que el comportamiento observable sea el mismo sin usar la herramienta
 * (CLAUDE.md §9).
 *
 * <p>No tiene {@code id}, {@code activated} ni {@code authorities}: esos los
 * decide el backend, nunca el cliente.
 */
public record RegistroRequest(

        @NotBlank
        @Size(min = 1, max = 50)
        @Pattern(regexp = "^[_.@A-Za-z0-9-]*$")
        String login,

        @NotBlank
        @Size(min = 4, max = 100)
        String password,

        @Size(max = 50)
        String firstName,

        @Size(max = 50)
        String lastName,

        @NotBlank
        @Email
        @Size(min = 5, max = 254)
        String email,

        @Size(max = 256)
        String imageUrl,

        @NotBlank
        @Size(min = 2, max = 10)
        String langKey

) {
}

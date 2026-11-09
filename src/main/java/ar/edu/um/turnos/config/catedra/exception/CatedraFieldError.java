package ar.edu.um.turnos.config.catedra.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un error de campo puntual dentro de un {@code application/problem+json}
 * con {@code code} {@code VALIDATION_ERROR} (§13 de la referencia).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraFieldError(String objectName, String field, String message) {
}

package ar.edu.um.turnos.auth.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta de {@code POST /api/authenticate}. El nombre del campo en JSON
 * es {@code id_token} (no {@code idToken}) a proposito: es el que usa la
 * {@code JWTToken} que genera JHipster, y mantenerlo es lo que hace que el
 * contrato sea observable-compatible sin usar la herramienta (CLAUDE.md §9).
 */
public record TokenResponse(

        @JsonProperty("id_token")
        String idToken

) {
}

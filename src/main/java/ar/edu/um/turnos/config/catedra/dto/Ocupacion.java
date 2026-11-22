package ar.edu.um.turnos.config.catedra.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una posicion no disponible de la agenda de un profesional (§8 de la
 * referencia). {@code status} es {@code CONFIRMED} o {@code HELD};
 * {@code expiresAt} solo tiene valor para {@code HELD}.
 */
public record Ocupacion(

        LocalDate date,

        LocalTime startTime,

        String status,

        Instant expiresAt

) {
}

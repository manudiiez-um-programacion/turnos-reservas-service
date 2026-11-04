package ar.edu.um.turnos.config.catedra.dto;

import java.time.LocalDate;

/**
 * Filtros, todos opcionales y combinables, de {@code GET /api/appointments}
 * (§11). Un campo en {@code null} se omite de la query en lugar de mandarse
 * vacio.
 */
public record ListarReservasFiltro(

        String status,

        LocalDate from,

        LocalDate to,

        String externalPatientId,

        Integer page,

        Integer size,

        String sort

) {
}

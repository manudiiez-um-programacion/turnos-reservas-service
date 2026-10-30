package ar.edu.um.turnos.health.infrastructure.web.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contrato HTTP de salida. Vive en {@code infrastructure}: ni el dominio ni la
 * capa de aplicacion lo conocen.
 *
 * <p>Campos en {@code camelCase} e instantes ISO-8601 en UTC, igual que el
 * contrato de la catedra.
 */
@Schema(description = "Estado operativo del servicio de turnos y reservas")
public record EstadoServicioResponse(

        @Schema(description = "Nombre del servicio", example = "turnos-reservas-service")
        String nombre,

        @Schema(description = "Version del artefacto", example = "0.0.1-SNAPSHOT")
        String version,

        @Schema(description = "Estado operativo", example = "UP")
        String estado,

        @Schema(description = "Instante de la consulta, ISO-8601 en UTC")
        Instant consultadoEn

) {
}

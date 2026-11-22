package ar.edu.um.turnos.health.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contrato HTTP de salida (#40). Solo el nombre del estado de cada
 * dependencia: nunca un mensaje de excepcion, un host o una credencial.
 */
@Schema(description = "Estado de cada dependencia externa del servicio")
public record EstadoDependenciasResponse(

        @Schema(description = "Base de datos propia", example = "DISPONIBLE")
        String baseDeDatos,

        @Schema(description = "Servicio central de la catedra", example = "DISPONIBLE")
        String catedra,

        @Schema(description = "Servicio de catalogo y sincronizacion", example = "DISPONIBLE")
        String catalogo

) {
}

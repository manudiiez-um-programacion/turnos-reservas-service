package ar.edu.um.turnos.config.catalogo.dto;

import java.util.List;

/**
 * Respuesta de
 * {@code GET /api/internal/professionals/{professionalId}/agenda} (#16,
 * docs/contratos/catalogo-turnos.md#4 del repo turnos-catalogo-sync).
 *
 * <p>No se persiste: es informacion vigente que se usa en memoria para la
 * operacion en curso (§4.2 del enunciado - este servicio no replica el
 * catalogo). {@code catalogVersion} es la trazabilidad de sobre que copia se
 * valido, pensada para guardarse junto al proceso de reserva que la
 * consulto (M10 en adelante), nunca como sustituto del catalogo vigente.
 */
public record AgendaProfesionalResponse(

        ProfesionalVigente professional,

        List<HorarioSemanalVigente> schedules,

        VersionCatalogoAplicada catalogVersion

) {
}

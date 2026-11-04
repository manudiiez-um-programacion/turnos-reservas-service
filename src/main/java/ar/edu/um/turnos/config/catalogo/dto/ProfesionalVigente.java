package ar.edu.um.turnos.config.catalogo.dto;

/**
 * Datos vigentes de un profesional segun la copia local del catalogo (#16).
 * {@code enabled} siempre llega en {@code true}: un profesional
 * deshabilitado no devuelve agenda, devuelve
 * {@code 409 PROFESSIONAL_DISABLED}.
 *
 * <p>Estos campos se pueden guardar como dato historico dentro de una
 * reserva (nombre y apellido al momento de reservar), pero nunca como
 * sustituto del catalogo vigente para habilitar una operacion nueva (§4.2
 * del enunciado).
 */
public record ProfesionalVigente(

        Long id,

        Long categoryId,

        String firstName,

        String lastName,

        boolean enabled

) {
}

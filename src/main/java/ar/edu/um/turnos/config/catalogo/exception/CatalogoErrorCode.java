package ar.edu.um.turnos.config.catalogo.exception;

/**
 * Codigo funcional estable que el catalogo informa en el {@code code} de un
 * {@code application/problem+json} al consultar la agenda vigente (#16,
 * docs/contratos/catalogo-turnos.md#6 del repo turnos-catalogo-sync),
 * traducido a un valor propio y tipado en lugar de que cada caso de uso
 * futuro (disponibilidad, holds) compare strings sueltos.
 *
 * <p>{@code PROFESSIONAL_NOT_FOUND} y {@code PROFESSIONAL_DISABLED} se
 * distinguen a proposito (404 vs 409 del otro lado): el primero es un dato
 * invalido (no hay nada que hacer), el segundo impide iniciar una operacion
 * nueva pero no invalida una reserva historica ya hecha con esos datos.
 *
 * <p>{@link #DESCONOCIDO} es el valor de resguardo tanto para un {@code code}
 * ausente (401/403 directos de Spring Security) como para uno que el
 * catalogo agregue mas adelante y este enum todavia no conozca: en los dos
 * casos el cliente sigue funcionando, solo que sin una traduccion especifica.
 */
public enum CatalogoErrorCode {

    AUTH_TOKEN_MISSING,
    AUTH_TOKEN_INVALID,
    AUTH_FORBIDDEN,
    PROFESSIONAL_NOT_FOUND,
    PROFESSIONAL_DISABLED,
    DESCONOCIDO;

    public static CatalogoErrorCode desde(String code) {
        if (code == null) {
            return DESCONOCIDO;
        }
        try {
            return valueOf(code);
        } catch (IllegalArgumentException codigoNoReconocido) {
            return DESCONOCIDO;
        }
    }

}

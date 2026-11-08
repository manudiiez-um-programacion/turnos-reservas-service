package ar.edu.um.turnos.health.domain.model;

/**
 * Estado de una dependencia individual del servicio (#40).
 *
 * <p>{@code DESCONOCIDO} es valido para una dependencia que se observa de
 * forma pasiva (la catedra, el catalogo): todavia no hubo ninguna llamada
 * real con la que informar un estado. La base de datos se verifica activa
 * en cada consulta, asi que nunca queda en este valor.
 */
public enum EstadoDependencia {
    DISPONIBLE,
    DEGRADADO,
    DESCONOCIDO
}

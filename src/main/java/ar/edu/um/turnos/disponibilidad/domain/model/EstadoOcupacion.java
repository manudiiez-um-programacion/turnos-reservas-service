package ar.edu.um.turnos.disponibilidad.domain.model;

/**
 * Vocabulario propio para el {@code status} de una ocupacion informada por la
 * catedra (§8 de la referencia): una reserva ya confirmada o un hold todavia
 * activo.
 *
 * <p>{@link #DESCONOCIDO} es el valor de resguardo si la catedra informa un
 * {@code status} distinto de los dos documentados: no rompe el mapeo, solo
 * avisa que la referencia cambio.
 */
public enum EstadoOcupacion {

    CONFIRMADA,
    RETENIDA,
    DESCONOCIDO;

    public static EstadoOcupacion desde(String status) {
        if (status == null) {
            return DESCONOCIDO;
        }
        return switch (status) {
            case "CONFIRMED" -> CONFIRMADA;
            case "HELD" -> RETENIDA;
            default -> DESCONOCIDO;
        };
    }

}

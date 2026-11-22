package ar.edu.um.turnos.reservas.application.exception;

/**
 * El filtro {@code status} de {@code GET /api/reservas} (#33) no corresponde
 * a ningun {@link ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva}
 * local. Error del cliente, no una inconsistencia del dominio.
 */
public class EstadoReservaInvalidoException extends RuntimeException {

    public EstadoReservaInvalidoException(String status) {
        super("el estado '%s' no es un estado valido de proceso de reserva".formatted(status));
    }

}

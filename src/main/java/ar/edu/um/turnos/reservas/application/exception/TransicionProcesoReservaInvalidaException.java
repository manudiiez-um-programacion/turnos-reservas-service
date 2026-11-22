package ar.edu.um.turnos.reservas.application.exception;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;

/**
 * Se pidio una transicion que {@link EstadoProcesoReserva#puedeTransicionarA}
 * no permite (#26, §7 del enunciado): un estado final que retrocede o una
 * transicion que la maquina de estados no contempla. Nunca se aplica
 * silenciosamente.
 */
public class TransicionProcesoReservaInvalidaException extends RuntimeException {

    public TransicionProcesoReservaInvalidaException(
            String reservationProcessId, EstadoProcesoReserva origen, EstadoProcesoReserva destino) {
        super("transicion invalida para el proceso %s: %s -> %s".formatted(reservationProcessId, origen, destino));
    }

}

package ar.edu.um.turnos.reservas.application.exception;

/**
 * El {@code holdId} no corresponde a ningun proceso local, o la catedra
 * informo {@code HOLD_NOT_FOUND} al confirmarlo (#28, §10 de la referencia):
 * en los dos casos es un dato invalido o un hold no recuperable, no hay nada
 * que reintentar.
 */
public class HoldNoEncontradoException extends RuntimeException {

    public HoldNoEncontradoException(String holdId) {
        super("no existe un proceso local para el hold %s".formatted(holdId));
    }

    public HoldNoEncontradoException(String holdId, Throwable causa) {
        super("la catedra no reconoce el hold %s".formatted(holdId), causa);
    }

}

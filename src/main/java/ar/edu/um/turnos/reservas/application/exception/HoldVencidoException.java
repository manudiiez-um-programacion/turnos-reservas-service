package ar.edu.um.turnos.reservas.application.exception;

/**
 * La catedra informo {@code HOLD_EXPIRED} al confirmar el hold (#28, §18.3
 * de la referencia): el reloj de {@code expiresAt} ya corrio y no se puede
 * extender. Cierra el proceso local y corta nuevos intentos de
 * confirmacion.
 */
public class HoldVencidoException extends RuntimeException {

    public HoldVencidoException(String holdId, Throwable causa) {
        super("el hold %s esta vencido".formatted(holdId), causa);
    }

}

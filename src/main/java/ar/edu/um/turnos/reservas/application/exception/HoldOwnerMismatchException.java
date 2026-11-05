package ar.edu.um.turnos.reservas.application.exception;

/**
 * La catedra informo {@code HOLD_OWNER_MISMATCH} al confirmar el hold (#28,
 * §10 de la referencia): el hold pertenece a otro {@code groupId} tecnico.
 * Es una inconsistencia que se registra, no se reintenta.
 */
public class HoldOwnerMismatchException extends RuntimeException {

    public HoldOwnerMismatchException(String holdId, Throwable causa) {
        super("el hold %s pertenece a otro groupId tecnico".formatted(holdId), causa);
    }

}

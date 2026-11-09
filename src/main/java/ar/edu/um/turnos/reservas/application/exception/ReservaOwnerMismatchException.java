package ar.edu.um.turnos.reservas.application.exception;

/**
 * La catedra informo {@code RESERVATION_OWNER_MISMATCH} al cancelar (#34,
 * §12 de la referencia): la reserva pertenece a otro {@code groupId}
 * tecnico. Es una inconsistencia que se registra, no se reintenta — distinta
 * de {@link ProcesoReservaAjenoException}, que es el chequeo local sobre el
 * usuario final.
 */
public class ReservaOwnerMismatchException extends RuntimeException {

    public ReservaOwnerMismatchException(String reservationProcessId, Throwable causa) {
        super("la reserva %s pertenece a otro groupId tecnico".formatted(reservationProcessId), causa);
    }

}

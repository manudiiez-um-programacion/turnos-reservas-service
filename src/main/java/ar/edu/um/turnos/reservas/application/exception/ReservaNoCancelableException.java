package ar.edu.um.turnos.reservas.application.exception;

/**
 * El proceso no esta en un estado cancelable (#34, §12 de la referencia):
 * localmente, cuando todavia no llego a
 * {@link ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva#CONFIRMADA}
 * (la unica transicion valida hacia {@code CANCELADA}); o la catedra informo
 * {@code RESERVATION_NOT_CANCELLABLE} porque su propio estado ya no es
 * {@code CONFIRMED} ni {@code CANCELLED}.
 */
public class ReservaNoCancelableException extends RuntimeException {

    public ReservaNoCancelableException(String reservationProcessId) {
        super("la reserva %s no esta en un estado cancelable".formatted(reservationProcessId));
    }

    public ReservaNoCancelableException(String reservationProcessId, Throwable causa) {
        super("la reserva %s no esta en un estado cancelable".formatted(reservationProcessId), causa);
    }

}

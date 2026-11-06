package ar.edu.um.turnos.reservas.application.exception;

/**
 * El {@code reservationProcessId} no corresponde a ningun proceso local, o la
 * catedra informo {@code RESERVATION_NOT_FOUND} al cancelar (#34, §12 de la
 * referencia). El segundo caso es una inconsistencia (el proceso local se
 * cree {@code CONFIRMADA} pero la catedra no lo reconoce): se registra y se
 * propaga sin tocar el estado local.
 */
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException(String reservationProcessId) {
        super("no existe un proceso local para la reserva %s".formatted(reservationProcessId));
    }

    public ReservaNoEncontradaException(String reservationProcessId, Throwable causa) {
        super("la catedra no reconoce la reserva %s".formatted(reservationProcessId), causa);
    }

}

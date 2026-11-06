package ar.edu.um.turnos.reservas.domain.ports.out;

import ar.edu.um.turnos.reservas.domain.model.CancelacionReservaCatedra;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

/**
 * Puerto de salida: cancela la reserva en la catedra
 * ({@code POST /api/appointments/{reservationProcessId}/cancel}, #34, §12
 * de la referencia). Recibe el {@link ProcesoReserva} completo para poder
 * construir mensajes de error con sus datos cuando la catedra informa un
 * codigo que los necesita.
 */
public interface CancelarReservaCatedraPort {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException
     * @throws ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException
     * @throws ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException
     */
    CancelacionReservaCatedra cancelar(ProcesoReserva proceso, String reason);

}

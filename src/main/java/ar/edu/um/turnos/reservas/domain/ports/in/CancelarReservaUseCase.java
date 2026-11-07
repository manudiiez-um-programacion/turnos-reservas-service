package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudCancelacionReserva;

/**
 * Puerto de entrada: cancela una reserva confirmada propia (#34, punto 14
 * del alcance obligatorio del enunciado y §12 de la referencia).
 */
public interface CancelarReservaUseCase {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException
     *         si el {@code reservationProcessId} no corresponde a ningun
     *         proceso local, o la catedra no lo reconoce.
     * @throws ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException
     *         si el usuario autenticado no es el dueño del proceso.
     * @throws ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException
     *         si el proceso no esta {@code CONFIRMADA} (salvo que ya este
     *         {@code CANCELADA}, que se trata como exito idempotente), o la
     *         catedra informa que su propio estado ya no es cancelable.
     * @throws ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException
     *         si la catedra informa que la reserva es de otro {@code groupId}
     *         tecnico: inconsistencia que se registra y se propaga.
     */
    ProcesoReserva cancelar(SolicitudCancelacionReserva solicitud);

}

package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;

/**
 * Puerto de entrada: confirma inicialmente el hold de un proceso de reserva
 * (#28, §10 de la referencia). Es el punto donde el proceso pasa de REST a
 * Kafka: la catedra crea una reserva {@code PHONE_PENDING} y publica
 * {@code AdditionalInformationRequested}.
 */
public interface ConfirmarHoldUseCase {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException
     *         si el {@code holdId} no corresponde a ningun proceso local, o
     *         la catedra lo informa como no reconocible ({@code HOLD_NOT_FOUND}).
     * @throws ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException
     *         si el usuario autenticado no es el dueño del proceso.
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldVencidoException
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException
     * @throws ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException
     * @throws ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException
     *         si el proceso ya no esta en un estado desde el que se pueda
     *         confirmar (por ejemplo, ya esta vencido o cancelado).
     */
    ProcesoReserva confirmar(SolicitudConfirmacionHold solicitud);

}

package ar.edu.um.turnos.reservas.domain.ports.out;

import ar.edu.um.turnos.reservas.domain.model.ConfirmacionHoldCatedra;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

/**
 * Puerto de salida: confirma inicialmente el bloqueo temporal en la catedra
 * ({@code POST /api/appointment-holds/{holdId}/confirm}, #28, §10 de la
 * referencia). Recibe el {@link ProcesoReserva} completo (y no solo el
 * {@code holdId}) para poder construir mensajes de error con el profesional,
 * la fecha y la hora del slot cuando la catedra informa un codigo que los
 * necesita.
 */
public interface ConfirmarHoldCatedraPort {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldVencidoException
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException
     * @throws ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldYaConfirmadoException
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException
     */
    ConfirmacionHoldCatedra confirmar(
            ProcesoReserva proceso, String externalPatientId, String patientFirstName, String patientLastName);

}

package ar.edu.um.turnos.reservas.domain.ports.out;

import ar.edu.um.turnos.reservas.domain.model.HoldCreado;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;

/**
 * Puerto de salida: pide el bloqueo temporal a la catedra
 * ({@code POST /api/appointment-holds}, #27, §9 de la referencia).
 */
public interface CrearHoldCatedraPort {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException
     */
    HoldCreado crear(SolicitudHold solicitud);

}

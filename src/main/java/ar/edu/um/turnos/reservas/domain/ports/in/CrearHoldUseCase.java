package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;

/**
 * Puerto de entrada: crea el bloqueo temporal del slot elegido (#27, punto 9
 * del alcance obligatorio del enunciado). Valida contra la agenda vigente
 * del catalogo antes de pedir el hold a la catedra, y persiste el proceso
 * de reserva nuevo con el resultado.
 */
public interface CrearHoldUseCase {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException
     *         si el profesional no existe en el catalogo.
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException
     *         si el profesional esta deshabilitado.
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException
     *         si la fecha y hora pedidas no corresponden a un slot habilitado
     *         de la agenda vigente (local) o la catedra lo rechaza por lo mismo.
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException
     *         si otro proceso ya tiene un hold activo sobre el mismo slot.
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException
     *         si el slot ya esta confirmado por otra reserva.
     */
    ProcesoReserva crear(SolicitudHold solicitud);

}

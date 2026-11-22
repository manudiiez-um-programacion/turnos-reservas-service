package ar.edu.um.turnos.reservas.domain.ports.out;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.turnos.reservas.domain.model.ProfesionalParaHold;

/**
 * Puerto de salida: valida, contra la agenda vigente del catalogo (#16), que
 * la fecha y hora pedidas correspondan a un slot habilitado de ese
 * profesional, antes de pedir el hold a la catedra (#27, §4.2 del enunciado:
 * la informacion vigente se pide antes de cada operacion). Devuelve los
 * datos del profesional para congelarlos como historico en el proceso de
 * reserva.
 */
public interface ValidarSlotCatalogoPort {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException
     *         si el profesional no existe.
     * @throws ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException
     *         si el profesional esta deshabilitado.
     * @throws ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException
     *         si la fecha y hora pedidas no corresponden a ningun slot
     *         habilitado de la agenda vigente.
     */
    ProfesionalParaHold validar(Long professionalId, LocalDate fecha, LocalTime horaInicio);

}

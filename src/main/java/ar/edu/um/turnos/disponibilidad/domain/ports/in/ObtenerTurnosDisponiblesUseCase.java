package ar.edu.um.turnos.disponibilidad.domain.ports.in;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.turnos.disponibilidad.domain.model.Slot;

/**
 * Puerto de entrada: los turnos libres de un profesional para la fecha que
 * elige el usuario (#24, §7 del enunciado — "la fecha y el profesional
 * elegidos por el usuario", en singular: una fecha puntual, no un rango).
 * Combina la agenda vigente del catalogo (#16) con las ocupaciones de la
 * catedra (#22) pedidas en cada invocacion, sin cachear ninguna de las dos:
 * un slot libre en esta respuesta puede dejar de estarlo al pedir el hold,
 * que es comportamiento esperado (#27).
 */
public interface ObtenerTurnosDisponiblesUseCase {

    List<Slot> obtener(Long professionalId, LocalDate fecha);

}

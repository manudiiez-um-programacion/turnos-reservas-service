package ar.edu.um.turnos.disponibilidad.domain.ports.in;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;

/**
 * Puerto de entrada: las ocupaciones de un profesional en un rango de fechas,
 * tal como las informa la catedra en este momento (#22, §8 de la
 * referencia). No se persisten: son el estado del momento, no un catalogo
 * propio.
 */
public interface ObtenerOcupacionesUseCase {

    List<Ocupacion> obtener(Long professionalId, LocalDate from, LocalDate to);

}

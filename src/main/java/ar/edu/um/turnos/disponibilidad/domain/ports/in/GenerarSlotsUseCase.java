package ar.edu.um.turnos.disponibilidad.domain.ports.in;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;

/**
 * Puerto de entrada: expande las reglas horarias semanales vigentes de un
 * profesional a los slots concretos de una fecha dada (#23). Logica de
 * dominio pura, sin puerto de salida: no necesita base de datos ni red, y
 * por eso es determinista y testeable en el mismo sentido que el resto del
 * {@code domain}.
 */
public interface GenerarSlotsUseCase {

    List<Slot> generar(List<ReglaHorariaSemanal> reglas, LocalDate fecha);

}

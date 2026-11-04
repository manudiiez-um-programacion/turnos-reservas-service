package ar.edu.um.turnos.disponibilidad.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un turno concreto de la agenda de un profesional, expandido a partir de
 * una {@link ReglaHorariaSemanal} (#23). {@code horaInicio} y
 * {@code horaFin} son horas locales de atencion, no instantes UTC.
 */
public record Slot(

        LocalDate fecha,

        LocalTime horaInicio,

        LocalTime horaFin

) {
}

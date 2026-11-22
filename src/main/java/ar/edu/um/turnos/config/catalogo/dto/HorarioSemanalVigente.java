package ar.edu.um.turnos.config.catalogo.dto;

import java.time.LocalTime;

/**
 * Un horario semanal habilitado de la agenda vigente de un profesional
 * (#16). Solo llegan los habilitados: la copia local del catalogo tambien
 * guarda los deshabilitados, pero el contrato no los publica como agenda
 * vigente. El orden de la lista que contiene estos horarios es parte del
 * contrato: por dia de la semana y, dentro del dia, por hora de inicio.
 */
public record HorarioSemanalVigente(

        Long id,

        String dayOfWeek,

        LocalTime startTime,

        LocalTime endTime,

        int slotDurationMinutes

) {
}

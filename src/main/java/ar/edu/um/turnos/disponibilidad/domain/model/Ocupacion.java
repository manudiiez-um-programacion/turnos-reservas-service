package ar.edu.um.turnos.disponibilidad.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una posicion no disponible de la agenda de un profesional, para una fecha y
 * hora de inicio dadas (§8 de la referencia). {@code expiraEn} solo tiene
 * valor cuando {@code estado} es {@link EstadoOcupacion#RETENIDA}.
 *
 * <p>Una ocupacion {@link EstadoOcupacion#RETENIDA} ya vencida segun
 * {@code expiraEn} sigue apareciendo hasta que la catedra la limpie. Este
 * slice no la filtra ni la trata como libre: el criterio seguro (#22) es
 * confiar en lo que informa la catedra en este momento y dejar que el
 * {@code POST} de hold sea la autoridad final sobre si el slot sigue
 * ocupado.
 */
public record Ocupacion(

        LocalDate fecha,

        LocalTime horaInicio,

        EstadoOcupacion estado,

        Instant expiraEn

) {
}

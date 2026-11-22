package ar.edu.um.turnos.disponibilidad.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Una regla semanal vigente de la agenda de un profesional (#16, contrato
 * {@code catalogo-turnos.md}), antes de expandirla a slots concretos (#23).
 *
 * <p>Se usa {@link DayOfWeek} directamente: sus siete constantes
 * ({@code MONDAY}..{@code SUNDAY}) ya son, letra por letra, los valores que
 * acepta el contrato, asi que no hace falta un enum propio que los traduzca.
 *
 * <p>{@code startTime} y {@code endTime} son horas locales de atencion, no
 * instantes UTC: a diferencia de un {@code expiresAt} de hold, no se
 * convierten.
 */
public record ReglaHorariaSemanal(

        DayOfWeek dayOfWeek,

        LocalTime startTime,

        LocalTime endTime,

        int slotDurationMinutes,

        boolean enabled

) {
}

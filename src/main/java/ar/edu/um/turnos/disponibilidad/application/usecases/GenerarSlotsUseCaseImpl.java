package ar.edu.um.turnos.disponibilidad.application.usecases;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.GenerarSlotsUseCase;

/**
 * Implementacion del caso de uso: solo expande las reglas habilitadas cuyo
 * {@code dayOfWeek} corresponde a la fecha pedida, en pasos de
 * {@code slotDurationMinutes} desde {@code startTime} hasta {@code endTime}
 * (#23). Un resto que no completa un slot no genera un slot parcial, y una
 * lista de reglas vacia (profesional deshabilitado o dia sin reglas) o sin
 * ninguna que coincida con la fecha devuelve una lista vacia, nunca un
 * error: es aritmetica simple sobre los datos recibidos.
 *
 * <p>Si dos reglas se solapan y generan el mismo slot exacto (mismo inicio
 * y fin), no se duplica: {@link Slot} es un record y su igualdad por valor
 * alcanza para deduplicar con un {@link Set}.
 */
@Component
public class GenerarSlotsUseCaseImpl implements GenerarSlotsUseCase {

    @Override
    public List<Slot> generar(List<ReglaHorariaSemanal> reglas, LocalDate fecha) {
        DayOfWeek diaSemana = fecha.getDayOfWeek();
        Set<Slot> slots = new LinkedHashSet<>();
        for (ReglaHorariaSemanal regla : reglas) {
            if (regla.enabled() && regla.dayOfWeek().equals(diaSemana)) {
                slots.addAll(expandir(regla, fecha));
            }
        }
        return slots.stream().sorted(Comparator.comparing(Slot::horaInicio)).toList();
    }

    private List<Slot> expandir(ReglaHorariaSemanal regla, LocalDate fecha) {
        List<Slot> slots = new ArrayList<>();
        int inicioMinutos = regla.startTime().toSecondOfDay() / 60;
        int finMinutos = regla.endTime().toSecondOfDay() / 60;
        int duracion = regla.slotDurationMinutes();
        for (int minuto = inicioMinutos; minuto + duracion <= finMinutos; minuto += duracion) {
            slots.add(new Slot(fecha, LocalTime.ofSecondOfDay(minuto * 60L), LocalTime.ofSecondOfDay((minuto + duracion) * 60L)));
        }
        return slots;
    }

}

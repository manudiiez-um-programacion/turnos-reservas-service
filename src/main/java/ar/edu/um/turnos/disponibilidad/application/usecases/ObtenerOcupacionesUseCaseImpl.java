package ar.edu.um.turnos.disponibilidad.application.usecases;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.disponibilidad.application.exception.RangoFechasInvalidoException;
import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerOcupacionesUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.out.ConsultarOcupacionesCatedraPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementacion del caso de uso: rechaza un rango invalido antes de llamar a
 * la catedra y, si la misma fecha y hora de inicio aparece mas de una vez en
 * la respuesta, deja prevalecer {@link EstadoOcupacion#CONFIRMADA} sobre
 * {@link EstadoOcupacion#RETENIDA} (#22, §8 de la referencia).
 */
@Component
@RequiredArgsConstructor
public class ObtenerOcupacionesUseCaseImpl implements ObtenerOcupacionesUseCase {

    private final ConsultarOcupacionesCatedraPort consultarOcupacionesCatedraPort;

    @Override
    public List<Ocupacion> obtener(Long professionalId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new RangoFechasInvalidoException(from, to);
        }
        List<Ocupacion> ocupaciones = consultarOcupacionesCatedraPort.consultar(professionalId, from, to);
        return deduplicarPorSlot(ocupaciones);
    }

    private List<Ocupacion> deduplicarPorSlot(List<Ocupacion> ocupaciones) {
        Map<Slot, Ocupacion> porSlot = new LinkedHashMap<>();
        for (Ocupacion ocupacion : ocupaciones) {
            Slot slot = new Slot(ocupacion.fecha(), ocupacion.horaInicio());
            Ocupacion existente = porSlot.get(slot);
            if (existente == null || existente.estado() != EstadoOcupacion.CONFIRMADA) {
                porSlot.put(slot, ocupacion);
            }
        }
        return List.copyOf(porSlot.values());
    }

    private record Slot(LocalDate fecha, LocalTime horaInicio) {
    }

}

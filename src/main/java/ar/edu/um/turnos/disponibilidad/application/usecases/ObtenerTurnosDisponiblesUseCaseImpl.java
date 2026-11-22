package ar.edu.um.turnos.disponibilidad.application.usecases;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.GenerarSlotsUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerOcupacionesUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerTurnosDisponiblesUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.out.ConsultarAgendaVigenteCatalogoPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementacion del caso de uso: pide la agenda vigente al catalogo y las
 * ocupaciones a la catedra en cada invocacion (#24, ninguna de las dos se
 * cachea), genera los slots de la fecha (#23) y les resta los que ya estan
 * ocupados.
 *
 * <p>Si la agenda vigente no genera ningun slot para la fecha (dia sin
 * reglas, o todas deshabilitadas), no hace falta pedir las ocupaciones: no
 * hay nada que restarles. Esto tambien evita una llamada a la catedra
 * innecesaria.
 */
@Component
@RequiredArgsConstructor
public class ObtenerTurnosDisponiblesUseCaseImpl implements ObtenerTurnosDisponiblesUseCase {

    private final ConsultarAgendaVigenteCatalogoPort consultarAgendaVigenteCatalogoPort;
    private final ObtenerOcupacionesUseCase obtenerOcupacionesUseCase;
    private final GenerarSlotsUseCase generarSlotsUseCase;

    @Override
    public List<Slot> obtener(Long professionalId, LocalDate fecha) {
        List<ReglaHorariaSemanal> reglas = consultarAgendaVigenteCatalogoPort.consultar(professionalId);
        List<Slot> slots = generarSlotsUseCase.generar(reglas, fecha);
        if (slots.isEmpty()) {
            return slots;
        }

        List<Ocupacion> ocupaciones = obtenerOcupacionesUseCase.obtener(professionalId, fecha, fecha);
        Set<LocalTime> horasOcupadas = ocupaciones.stream().map(Ocupacion::horaInicio).collect(Collectors.toSet());

        return slots.stream().filter(slot -> !horasOcupadas.contains(slot.horaInicio())).toList();
    }

}

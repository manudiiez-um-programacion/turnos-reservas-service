package ar.edu.um.turnos.disponibilidad.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.GenerarSlotsUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerOcupacionesUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerTurnosDisponiblesUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice {@code disponibilidad}: es lo unico que la
 * infraestructura (el controlador de #24) conoce. Nadie fuera de este
 * paquete habla con un caso de uso ni con un puerto de salida directamente.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final ObtenerOcupacionesUseCase obtenerOcupacionesUseCase;
    private final GenerarSlotsUseCase generarSlotsUseCase;
    private final ObtenerTurnosDisponiblesUseCase obtenerTurnosDisponiblesUseCase;

    public List<Ocupacion> obtenerOcupaciones(Long professionalId, LocalDate from, LocalDate to) {
        return obtenerOcupacionesUseCase.obtener(professionalId, from, to);
    }

    public List<Slot> generarSlots(List<ReglaHorariaSemanal> reglas, LocalDate fecha) {
        return generarSlotsUseCase.generar(reglas, fecha);
    }

    public List<Slot> obtenerTurnosDisponibles(Long professionalId, LocalDate fecha) {
        return obtenerTurnosDisponiblesUseCase.obtener(professionalId, fecha);
    }

}

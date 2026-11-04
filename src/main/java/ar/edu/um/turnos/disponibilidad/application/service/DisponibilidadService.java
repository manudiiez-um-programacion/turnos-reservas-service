package ar.edu.um.turnos.disponibilidad.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerOcupacionesUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice {@code disponibilidad}: es lo unico que la
 * infraestructura (y, mas adelante, el calculo de slots de #23 y el
 * controlador de #24) conoce. Nadie fuera de este paquete habla con un caso
 * de uso ni con un puerto de salida directamente.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final ObtenerOcupacionesUseCase obtenerOcupacionesUseCase;

    public List<Ocupacion> obtenerOcupaciones(Long professionalId, LocalDate from, LocalDate to) {
        return obtenerOcupacionesUseCase.obtener(professionalId, from, to);
    }

}

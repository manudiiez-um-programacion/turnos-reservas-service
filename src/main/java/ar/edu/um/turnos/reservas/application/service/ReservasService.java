package ar.edu.um.turnos.reservas.application.service;

import java.time.Instant;
import org.springframework.stereotype.Service;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice {@code reservas}: es lo unico que la infraestructura
 * (el oyente de Kafka, y mas adelante el controlador REST de holds y
 * reservas) conoce. Nadie fuera de este paquete habla con un caso de uso ni
 * con un puerto de salida directamente.
 */
@Service
@RequiredArgsConstructor
public class ReservasService {

    private final ProcesarEventoAccionUseCase procesarEventoAccionUseCase;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;

    public void procesarEventoAccion(EventoAccionReserva evento) {
        procesarEventoAccionUseCase.procesar(evento);
    }

    public ProcesoReserva transicionar(ProcesoReserva proceso, EstadoProcesoReserva nuevoEstado, Instant ocurridoEn) {
        return transicionarProcesoReservaUseCase.transicionar(proceso, nuevoEstado, ocurridoEn);
    }

}

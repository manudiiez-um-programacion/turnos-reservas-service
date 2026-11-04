package ar.edu.um.turnos.reservas.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
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

    public void procesarEventoAccion(EventoAccionReserva evento) {
        procesarEventoAccionUseCase.procesar(evento);
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.TransicionProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion de la maquina de estados (#26): valida contra
 * {@link EstadoProcesoReserva#puedeTransicionarA} antes de mutar el proceso,
 * y deja la transicion aplicada en su historial. No persiste nada: eso es
 * responsabilidad de quien llama (#27, #28, ...), que decide cuando guardar
 * el resultado.
 */
@Slf4j
@Component
public class TransicionarProcesoReservaUseCaseImpl implements TransicionarProcesoReservaUseCase {

    @Override
    public ProcesoReserva transicionar(ProcesoReserva proceso, EstadoProcesoReserva nuevoEstado, Instant ocurridoEn) {
        EstadoProcesoReserva estadoActual = proceso.getEstado();

        if (!estadoActual.puedeTransicionarA(nuevoEstado)) {
            log.warn("Transicion rechazada para el proceso {}: {} -> {}",
                    proceso.getReservationProcessId(), estadoActual, nuevoEstado);
            throw new TransicionProcesoReservaInvalidaException(
                    proceso.getReservationProcessId(), estadoActual, nuevoEstado);
        }

        proceso.setEstado(nuevoEstado);
        proceso.getHistorial().add(new TransicionProcesoReserva(nuevoEstado, ocurridoEn));

        log.info("Proceso {} transiciono {} -> {}", proceso.getReservationProcessId(), estadoActual, nuevoEstado);

        return proceso;
    }

}

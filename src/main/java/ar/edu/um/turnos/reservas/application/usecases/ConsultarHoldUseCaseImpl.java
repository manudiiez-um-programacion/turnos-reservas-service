package ar.edu.um.turnos.reservas.application.usecases;

import java.util.Objects;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.ConsultarHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementacion del caso de uso (#29): busca el proceso por {@code holdId} y
 * valida que el usuario autenticado sea su dueño, con las mismas excepciones
 * que {@link ConfirmarHoldUseCaseImpl} para que el cliente las trate igual.
 */
@Component
@RequiredArgsConstructor
public class ConsultarHoldUseCaseImpl implements ConsultarHoldUseCase {

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Override
    public ProcesoReserva consultar(String holdId, Long usuarioId) {
        Objects.requireNonNull(holdId, "el holdId no puede ser null");
        Objects.requireNonNull(usuarioId, "el usuarioId no puede ser null");

        ProcesoReserva proceso = procesoReservaRepositoryPort.buscarPorHoldId(holdId)
                .orElseThrow(() -> new HoldNoEncontradoException(holdId));

        if (!proceso.getUsuarioId().equals(usuarioId)) {
            throw new ProcesoReservaAjenoException(holdId, usuarioId);
        }

        return proceso;
    }

}

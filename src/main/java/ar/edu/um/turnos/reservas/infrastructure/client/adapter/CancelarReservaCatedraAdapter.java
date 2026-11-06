package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.CancelacionReservaResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException;
import ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException;
import ar.edu.um.turnos.reservas.domain.model.CancelacionReservaCatedra;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.CancelarReservaCatedraPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: cancela la reserva en {@link CatedraClient}
 * (#34, §12 de la referencia) y traduce los errores funcionales de la
 * matriz de §13.1 a las excepciones propias de este slice. Un error que esa
 * matriz no documenta para este endpoint se deja propagar sin traducir.
 */
@Component
@RequiredArgsConstructor
public class CancelarReservaCatedraAdapter implements CancelarReservaCatedraPort {

    private final CatedraClient catedraClient;

    @Override
    public CancelacionReservaCatedra cancelar(ProcesoReserva proceso, String reason) {
        try {
            CancelacionReservaResponse respuesta = catedraClient.cancelarReserva(proceso.getReservationProcessId(), reason);
            return new CancelacionReservaCatedra(respuesta.reservationId(), respuesta.status(), respuesta.cancelledAt());
        } catch (CatedraClientErrorException error) {
            throw traducirError(proceso, error);
        }
    }

    private RuntimeException traducirError(ProcesoReserva proceso, CatedraClientErrorException error) {
        return switch (error.getErrorCode()) {
            case RESERVATION_NOT_FOUND -> new ReservaNoEncontradaException(proceso.getReservationProcessId(), error);
            case RESERVATION_OWNER_MISMATCH -> new ReservaOwnerMismatchException(proceso.getReservationProcessId(), error);
            case RESERVATION_NOT_CANCELLABLE -> new ReservaNoCancelableException(proceso.getReservationProcessId(), error);
            default -> error;
        };
    }

}

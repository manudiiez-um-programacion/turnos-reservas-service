package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

/**
 * Resultado de cancelar la reserva en la catedra
 * ({@link ar.edu.um.turnos.reservas.domain.ports.out.CancelarReservaCatedraPort},
 * #34, §12 de la referencia). {@code cancelledAt} es siempre el que informa
 * la catedra.
 */
public record CancelacionReservaCatedra(

        Long reservationId,

        String status,

        Instant cancelledAt

) {
}

package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

/**
 * Una reserva tal como la tiene la catedra al reconciliar (#36, §18.4 de la
 * referencia): resultado de buscarla por {@code reservationProcessId} dentro
 * de {@code GET /api/appointments} filtrado por {@code externalPatientId}
 * (la cátedra no permite filtrar directo por {@code reservationProcessId}).
 * Su sola presencia ya es la señal de que una operacion con efectos se aplico
 * del lado de la catedra, aunque la respuesta nunca haya llegado a este
 * servicio.
 */
public record ReservaCatedraEncontrada(

        Long reservationId,

        String status,

        Instant cancelledAt,

        String cancellationReason

) {
}

package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import java.time.Instant;

/**
 * Respuesta {@code 201} de {@code POST /api/holds} y de los demas endpoints
 * del controlador (#27). {@code reservationId}, {@code reason} y {@code
 * reasonMessage} solo tienen valor una vez que llego un resultado final
 * (#31): le dejan al usuario consultar la reserva confirmada o el motivo de
 * un rechazo, vencimiento o proceso invalido.
 */
public record HoldResponse(

        String holdId,

        String reservationProcessId,

        String status,

        Instant expiresAt,

        Long reservationId,

        String reason,

        String reasonMessage

) {
}

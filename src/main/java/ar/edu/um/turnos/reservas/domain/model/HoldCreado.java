package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

/**
 * Resultado de crear el hold en la catedra
 * ({@link ar.edu.um.turnos.reservas.domain.ports.out.CrearHoldCatedraPort},
 * #27). {@code expiresAt} es siempre el que informa la catedra: conservarlo
 * localmente no extiende el TTL (§9 de la referencia).
 */
public record HoldCreado(

        String holdId,

        String reservationProcessId,

        Instant expiresAt

) {
}

package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

/**
 * Resultado de confirmar inicialmente el hold en la catedra
 * ({@link ar.edu.um.turnos.reservas.domain.ports.out.ConfirmarHoldCatedraPort},
 * #28, §10 de la referencia). {@code expiresAt} es siempre el que informa la
 * catedra: conservarlo localmente no extiende el TTL (§9 de la referencia).
 */
public record ConfirmacionHoldCatedra(

        String reservationProcessId,

        String status,

        Instant expiresAt

) {
}

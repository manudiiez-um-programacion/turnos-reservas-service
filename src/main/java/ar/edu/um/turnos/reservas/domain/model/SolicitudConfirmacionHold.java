package ar.edu.um.turnos.reservas.domain.model;

import lombok.Builder;

/**
 * Entrada de {@link ar.edu.um.turnos.reservas.domain.ports.in.ConfirmarHoldUseCase}
 * (#28). {@code usuarioId} y {@code externalPatientId} ya vienen resueltos
 * del usuario autenticado: nunca se construyen a partir de un valor que
 * mande el cliente (§4.2 del enunciado).
 */
@Builder
public record SolicitudConfirmacionHold(

        String holdId,

        Long usuarioId,

        String externalPatientId,

        String patientFirstName,

        String patientLastName

) {
}

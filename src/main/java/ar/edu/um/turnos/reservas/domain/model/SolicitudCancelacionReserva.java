package ar.edu.um.turnos.reservas.domain.model;

import lombok.Builder;

/**
 * Entrada de {@link ar.edu.um.turnos.reservas.domain.ports.in.CancelarReservaUseCase}
 * (#34). {@code usuarioId} ya viene resuelto del usuario autenticado: nunca
 * se construye a partir de un valor que mande el cliente (§4.2 del
 * enunciado).
 */
@Builder
public record SolicitudCancelacionReserva(

        String reservationProcessId,

        Long usuarioId,

        String reason

) {
}

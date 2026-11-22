package ar.edu.um.turnos.reservas.domain.model;

import lombok.Builder;

/**
 * Entrada de {@link ar.edu.um.turnos.reservas.domain.ports.in.EnviarTelefonoUseCase}
 * (#30). {@code usuarioId} ya viene resuelto del usuario autenticado: nunca
 * se construye a partir de un valor que mande el cliente (§4.2 del
 * enunciado). {@code telefono} todavia es el valor crudo que mando el
 * usuario: la normalizacion y validacion son responsabilidad del caso de uso.
 */
@Builder
public record SolicitudEnvioTelefono(

        String holdId,

        Long usuarioId,

        String telefono

) {
}

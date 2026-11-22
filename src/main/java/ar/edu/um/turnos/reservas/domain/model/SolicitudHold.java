package ar.edu.um.turnos.reservas.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Builder;

/**
 * Entrada de {@link ar.edu.um.turnos.reservas.domain.ports.in.CrearHoldUseCase}
 * (#27). {@code usuarioId} y {@code externalPatientId} ya vienen resueltos
 * del usuario autenticado: nunca se construyen a partir de un valor que
 * mande el cliente (§4.2 del enunciado).
 */
@Builder
public record SolicitudHold(

        Long professionalId,

        LocalDate fecha,

        LocalTime horaInicio,

        Long usuarioId,

        String externalPatientId

) {
}

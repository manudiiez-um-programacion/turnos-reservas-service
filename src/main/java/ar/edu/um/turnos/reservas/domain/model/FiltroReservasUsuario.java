package ar.edu.um.turnos.reservas.domain.model;

import java.time.LocalDate;

import lombok.Builder;

/**
 * Entrada de {@link ar.edu.um.turnos.reservas.domain.ports.in.ConsultarReservasUseCase}
 * (#33). {@code usuarioId} ya viene resuelto del usuario autenticado: nunca
 * se construye a partir de un id que mande el cliente (§4.2 del enunciado),
 * y es el unico campo obligatorio de este filtro.
 *
 * <p>{@code estado}, {@code desde} y {@code hasta} son opcionales y
 * combinables. {@code pagina} y {@code tamanio} llegan ya saneados por el
 * caso de uso: nunca negativos ni por fuera del maximo permitido.
 */
@Builder(toBuilder = true)
public record FiltroReservasUsuario(

        Long usuarioId,

        EstadoProcesoReserva estado,

        LocalDate desde,

        LocalDate hasta,

        int pagina,

        int tamanio

) {
}

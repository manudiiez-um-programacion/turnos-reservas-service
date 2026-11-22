package ar.edu.um.turnos.reservas.domain.ports.in;

import java.time.Instant;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

/**
 * Puerto de entrada: aplica una transicion de estado sobre un
 * {@link ProcesoReserva} (#26). Logica de dominio pura, sin puerto de
 * salida propio: no decide cuando persistir, solo si la transicion pedida es
 * valida.
 */
public interface TransicionarProcesoReservaUseCase {

    /**
     * Aplica la transicion y la agrega al historial del proceso.
     *
     * @throws ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException
     *         si {@code nuevoEstado} no es alcanzable desde el estado actual
     *         (incluye intentar salir de un estado final no previsto para
     *         ello): se rechaza en vez de aplicarse silenciosamente.
     */
    ProcesoReserva transicionar(ProcesoReserva proceso, EstadoProcesoReserva nuevoEstado, Instant ocurridoEn);

}

package ar.edu.um.turnos.reservas.domain.ports.out;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;

/**
 * Puerto de salida: deduplicacion por {@code eventId} (#20, §15.1 y §18.1 de
 * la referencia). El dominio no sabe que hay una tabla ni una restriccion de
 * base por detras, solo que necesita saber si un evento ya se proceso.
 */
public interface RegistrarEventoProcesadoPort {

    /**
     * Intenta registrar el evento como procesado.
     *
     * @return {@code true} si es la primera vez que se ve este
     *         {@code eventId} (hay que ejecutar su efecto); {@code false} si
     *         ya estaba registrado (es un duplicado y el efecto no se repite).
     */
    boolean intentarRegistrar(EventoAccionReserva evento);

}

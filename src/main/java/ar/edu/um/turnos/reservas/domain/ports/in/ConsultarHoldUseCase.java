package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

/**
 * Puerto de entrada: consulta el estado local del proceso de reserva de un
 * hold (#29, criterio de aceptacion "el usuario puede consultar que su
 * proceso esta esperando el telefono"). Solo lectura: no dispara ninguna
 * llamada a la catedra ni transiciona el proceso.
 */
public interface ConsultarHoldUseCase {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException
     *         si el {@code holdId} no corresponde a ningun proceso local.
     * @throws ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException
     *         si el usuario autenticado no es el dueño del proceso.
     */
    ProcesoReserva consultar(String holdId, Long usuarioId);

}

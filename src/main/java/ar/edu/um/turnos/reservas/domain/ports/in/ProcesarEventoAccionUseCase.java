package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;

/**
 * Puerto de entrada: que puede hacer la aplicacion con un evento de accion de
 * reserva ya deserializado y correlacionado.
 *
 * <p>Una interfaz por caso de uso, con un solo metodo. Es lo que mantiene los
 * casos de uso chicos y testeables de forma aislada.
 */
public interface ProcesarEventoAccionUseCase {

    void procesar(EventoAccionReserva evento);

}

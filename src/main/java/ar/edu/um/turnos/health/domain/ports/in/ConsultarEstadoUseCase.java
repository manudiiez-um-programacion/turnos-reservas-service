package ar.edu.um.turnos.health.domain.ports.in;

import ar.edu.um.turnos.health.domain.model.EstadoServicio;

/**
 * Puerto de entrada: que puede hacer la aplicacion.
 *
 * <p>Una interfaz por caso de uso, con un solo metodo. Es lo que mantiene los
 * casos de uso chicos y testeables de forma aislada.
 */
public interface ConsultarEstadoUseCase {

    EstadoServicio consultar();

}

package ar.edu.um.turnos.reservas.domain.ports.in;

/**
 * Puerto de entrada: recupera los procesos de reserva que un reinicio del
 * servicio puede haber dejado a medio camino (#37, §4.2 y §8 del enunciado).
 * Lo dispara la infraestructura al terminar de arrancar, nunca un endpoint
 * REST: no hay nada que un usuario final tenga que pedir para esto.
 */
public interface RecuperarProcesosPendientesUseCase {

    void recuperar();

}

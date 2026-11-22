package ar.edu.um.turnos.reservas.domain.ports.in;

/**
 * Puerto de entrada: cierra como {@code VENCIDA} los holds y procesos de
 * reserva cuyo {@code expiresAt} ya paso (#38, §7 y §9 de la referencia:
 * conservar el TTL localmente no lo extiende). Lo dispara la tarea
 * programada de infraestructura y, al arrancar, la recuperacion de procesos
 * pendientes (#37); no hay endpoint REST para esto.
 */
public interface CerrarProcesosVencidosUseCase {

    /** @return cuantos procesos se cerraron como vencidos en esta corrida. */
    int cerrarVencidos();

}

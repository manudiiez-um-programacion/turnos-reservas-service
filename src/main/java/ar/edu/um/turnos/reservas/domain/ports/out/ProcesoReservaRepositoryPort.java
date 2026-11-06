package ar.edu.um.turnos.reservas.domain.ports.out;

import java.util.Optional;

import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

/**
 * Puerto de salida: persistencia de {@link ProcesoReserva} (#26). El dominio
 * no sabe que hay una tabla por detras, solo que el proceso sobrevive a
 * reinicios y se puede recuperar por los identificadores que lo
 * correlacionan con REST ({@code holdId}) y con Kafka
 * ({@code reservationProcessId}, clave de los eventos de
 * {@code alumnos.turnos.acciones.{groupId}}).
 */
public interface ProcesoReservaRepositoryPort {

    ProcesoReserva guardar(ProcesoReserva proceso);

    Optional<ProcesoReserva> buscarPorReservationProcessId(String reservationProcessId);

    Optional<ProcesoReserva> buscarPorHoldId(String holdId);

    /** Pagina de procesos del {@code usuarioId} del filtro, nunca de otro (#33). */
    PaginaProcesoReserva buscarPorUsuario(FiltroReservasUsuario filtro);

}

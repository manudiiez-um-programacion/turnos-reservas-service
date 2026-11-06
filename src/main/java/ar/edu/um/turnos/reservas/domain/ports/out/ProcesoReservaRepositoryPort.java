package ar.edu.um.turnos.reservas.domain.ports.out;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

    /**
     * El proceso no final y no vencido (segun {@code ahora}) del mismo
     * {@code usuarioId}, {@code professionalId}, {@code fecha} y
     * {@code horaInicio}, si existe (#36): evita crear un segundo hold para
     * un slot que ya tiene un proceso vigente.
     */
    Optional<ProcesoReserva> buscarVigentePorSlot(
            Long usuarioId, Long professionalId, LocalDate fecha, LocalTime horaInicio, Instant ahora);

    /**
     * Todos los procesos en un estado no final (#37): los que un reinicio
     * del servicio puede haber dejado a medio camino.
     */
    List<ProcesoReserva> listarNoFinales();

}

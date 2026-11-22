package ar.edu.um.turnos.reservas.domain.ports.out;

import java.util.Optional;

import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;

/**
 * Puerto de salida: busca una reserva en la catedra por
 * {@code reservationProcessId} para reconciliar un timeout o un error de
 * infraestructura en una operacion con efectos (#36, §18.4 de la
 * referencia). Usa {@code GET /api/appointments} filtrado por
 * {@code externalPatientId} porque ese endpoint no admite filtrar
 * directamente por {@code reservationProcessId}.
 */
public interface ConsultarReservaCatedraPort {

    Optional<ReservaCatedraEncontrada> buscarPorReservationProcessId(String externalPatientId, String reservationProcessId);

}

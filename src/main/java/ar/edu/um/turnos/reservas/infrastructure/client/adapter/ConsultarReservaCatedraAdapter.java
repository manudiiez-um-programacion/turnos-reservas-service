package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import java.util.Optional;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasFiltro;
import ar.edu.um.turnos.config.catedra.dto.ReservaResponse;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;
import ar.edu.um.turnos.reservas.domain.ports.out.ConsultarReservaCatedraPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: busca la reserva en {@link CatedraClient}
 * (#36) filtrando {@code GET /api/appointments} por {@code externalPatientId}
 * y despues eligiendo, del lado de este servicio, la que coincide con el
 * {@code reservationProcessId} que se quiere reconciliar.
 *
 * <p>El tamaño de pagina pedido es generoso a proposito: alcanza para el
 * volumen de reservas de un usuario final de este proyecto. Un usuario con
 * mas reservas que {@code TAMANIO_PAGINA} para el mismo {@code
 * externalPatientId} quedaria fuera de esta busqueda puntual; es una
 * limitacion conocida y no el caso general que #36 necesita cubrir.
 */
@Component
@RequiredArgsConstructor
public class ConsultarReservaCatedraAdapter implements ConsultarReservaCatedraPort {

    private static final int TAMANIO_PAGINA = 100;

    private final CatedraClient catedraClient;

    @Override
    public Optional<ReservaCatedraEncontrada> buscarPorReservationProcessId(String externalPatientId, String reservationProcessId) {
        ListarReservasFiltro filtro = new ListarReservasFiltro(null, null, null, externalPatientId, 0, TAMANIO_PAGINA, null);
        return catedraClient.listarReservas(filtro).reservas().stream()
                .filter(reserva -> reservationProcessId.equals(reserva.reservationProcessId()))
                .findFirst()
                .map(this::aReservaCatedraEncontrada);
    }

    private ReservaCatedraEncontrada aReservaCatedraEncontrada(ReservaResponse reserva) {
        return new ReservaCatedraEncontrada(
                reserva.reservationId(), reserva.status(), reserva.cancelledAt(), reserva.cancellationReason());
    }

}

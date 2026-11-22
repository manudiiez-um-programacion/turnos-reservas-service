package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;

/**
 * Puerto de entrada: consulta las reservas y procesos de reserva propios del
 * usuario autenticado (#33, §4.2 del enunciado). Responde siempre desde el
 * estado local — nunca consulta a la catedra ni al catalogo — asi que sigue
 * funcionando con el catalogo caido. Solo lectura: no transiciona ningun
 * proceso.
 */
public interface ConsultarReservasUseCase {

    PaginaProcesoReserva consultar(FiltroReservasUsuario filtro);

}

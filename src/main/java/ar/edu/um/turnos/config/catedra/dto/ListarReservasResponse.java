package ar.edu.um.turnos.config.catedra.dto;

import java.util.List;

/**
 * Pagina de reservas de {@code GET /api/appointments} (§11). El body es un
 * array plano; {@code totalCount} viene del header {@code X-Total-Count} que
 * manda la catedra para la paginacion Spring/JHipster, asi que el cliente lo
 * expone aparte en lugar de obligar a quien lo use a leer headers HTTP.
 */
public record ListarReservasResponse(

        List<ReservaResponse> reservas,

        long totalCount

) {
}

package ar.edu.um.turnos.reservas.infrastructure.web.dto;

import java.util.List;

/**
 * Respuesta {@code 200} de {@code GET /api/reservas} (#33): una pagina
 * explicita en el cuerpo en lugar de headers como {@code X-Total-Count}, para
 * que un cliente que no lee headers (como KMP) pueda calcular paginacion
 * igual.
 */
public record ReservasPaginaResponse(

        List<ReservaResponse> content,

        long totalElements,

        int page,

        int size

) {
}

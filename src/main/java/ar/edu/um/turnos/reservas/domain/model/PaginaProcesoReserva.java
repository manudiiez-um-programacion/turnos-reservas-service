package ar.edu.um.turnos.reservas.domain.model;

import java.util.List;

/**
 * Salida de {@link ar.edu.um.turnos.reservas.domain.ports.in.ConsultarReservasUseCase}
 * (#33): una pagina de {@link ProcesoReserva} del usuario autenticado, con el
 * total de elementos que coinciden con el filtro (sin paginar) para que el
 * cliente pueda calcular cuantas paginas quedan.
 */
public record PaginaProcesoReserva(

        List<ProcesoReserva> contenido,

        long totalElementos

) {
}

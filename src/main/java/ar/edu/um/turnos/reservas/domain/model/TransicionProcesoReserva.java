package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

/**
 * Un instante del historial de un {@link ProcesoReserva} (#26): a que estado
 * paso y cuando. No es el proceso completo en ese momento, solo el punto de
 * la transicion — alcanza para reconstruir "cuando paso cada cosa" sin
 * necesitar una columna por estado.
 */
public record TransicionProcesoReserva(

        EstadoProcesoReserva estado,

        Instant ocurridoEn

) {
}

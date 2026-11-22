package ar.edu.um.turnos.reservas.domain.model;

/**
 * Nombre y apellido vigentes del profesional segun el catalogo, devueltos
 * por {@link ar.edu.um.turnos.reservas.domain.ports.out.ValidarSlotCatalogoPort}
 * (#27) para congelarlos como dato historico del {@link ProcesoReserva}
 * (§4.2 del enunciado: nunca como catalogo vigente para operaciones
 * futuras).
 */
public record ProfesionalParaHold(

        String nombre,

        String apellido

) {
}

package ar.edu.um.turnos.reservas.application.exception;

/**
 * El usuario autenticado no es el dueño del proceso de reserva (#28, §4.2
 * del enunciado): un usuario jamas puede ver ni modificar lo de otro. Chequeo
 * puramente local, distinto de {@link HoldOwnerMismatchException} (que es
 * sobre el {@code groupId} tecnico que conoce la catedra, no sobre el usuario
 * final).
 */
public class ProcesoReservaAjenoException extends RuntimeException {

    public ProcesoReservaAjenoException(String holdId, Long usuarioId) {
        super("el usuario %s no es dueño del proceso del hold %s".formatted(usuarioId, holdId));
    }

}

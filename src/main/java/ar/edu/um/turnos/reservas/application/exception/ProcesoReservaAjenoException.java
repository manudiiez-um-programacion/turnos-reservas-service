package ar.edu.um.turnos.reservas.application.exception;

/**
 * El usuario autenticado no es el dueño del proceso de reserva (#28, §4.2
 * del enunciado): un usuario jamas puede ver ni modificar lo de otro. Chequeo
 * puramente local, distinto de {@link HoldOwnerMismatchException} y de
 * {@link ReservaOwnerMismatchException} (que son sobre el {@code groupId}
 * tecnico que conoce la catedra, no sobre el usuario final).
 *
 * <p>{@code identificador} es el {@code holdId} o el {@code reservationProcessId}
 * segun la operacion (#28, #29, #30, #34): cualquiera de los dos identifica
 * el proceso ajeno igual de bien.
 */
public class ProcesoReservaAjenoException extends RuntimeException {

    public ProcesoReservaAjenoException(String identificador, Long usuarioId) {
        super("el usuario %s no es dueño del proceso %s".formatted(usuarioId, identificador));
    }

}

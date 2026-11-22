package ar.edu.um.turnos.reservas.infrastructure.messaging.exception;

/**
 * Un mensaje de {@code alumnos.turnos.acciones.{groupId}} que no se pudo
 * interpretar: JSON invalido o falta un campo comun obligatorio
 * ({@code eventId}, {@code eventType}, {@code reservationProcessId} o
 * {@code schemaVersion}).
 *
 * <p>No es lo mismo que un {@code eventType} desconocido o una
 * {@code schemaVersion} no soportada (eso el contrato lo permite y se
 * descarta sin reintentar, §15.1): esta excepcion es un evento que ni
 * siquiera se puede leer como lo que dice ser, y por eso si entra en la
 * politica de reintentos acotados del oyente (#18).
 */
public class EventoAccionInvalidoException extends RuntimeException {

    public EventoAccionInvalidoException(String mensaje) {
        super(mensaje);
    }

    public EventoAccionInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

}

package ar.edu.um.turnos.reservas.application.exception;

/**
 * Se agotaron los reintentos de telefono permitidos para el proceso (#31,
 * §18.4 de la referencia: "los reintentos deben ser limitados y
 * observables"). No cierra el proceso: la catedra sigue siendo quien decide
 * su destino final via {@code AppointmentProcessExpired} cuando venza el
 * plazo; esta excepcion solo corta nuevos intentos locales antes de eso.
 */
public class LimiteReintentosTelefonoException extends RuntimeException {

    public LimiteReintentosTelefonoException(String reservationProcessId, int maximoIntentos) {
        super("el proceso %s alcanzo el maximo de %d intentos de telefono"
                .formatted(reservationProcessId, maximoIntentos));
    }

}

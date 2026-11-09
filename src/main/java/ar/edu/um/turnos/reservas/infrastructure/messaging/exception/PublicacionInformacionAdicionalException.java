package ar.edu.um.turnos.reservas.infrastructure.messaging.exception;

/**
 * La publicación de {@code AdditionalInformationSubmitted} no se pudo
 * confirmar (#19): ni el broker la aceptó a tiempo, ni se puede asumir que
 * llegó. Quien dispare el envío (#30) decide si reintenta con un
 * {@code eventId} nuevo o reconcilia por {@code reservationProcessId}.
 */
public class PublicacionInformacionAdicionalException extends RuntimeException {

    public PublicacionInformacionAdicionalException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

}

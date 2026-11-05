package ar.edu.um.turnos.reservas.application.exception;

/**
 * El {@code expiresAt} del proceso (guardado del ultimo
 * {@code AdditionalInformationRequested} o {@code AdditionalInformationRejected})
 * ya paso al intentar enviar el telefono (#30, §18.3 de la referencia): se
 * cierra el proceso local como vencido en lugar de publicar un intento que
 * la catedra de todas formas va a rechazar.
 *
 * <p>Distinta de {@link HoldVencidoException}: esa es sobre el vencimiento
 * del hold que informa la catedra al confirmar; esta es una decision local,
 * previa a cualquier llamada, sobre el plazo para responder un pedido de
 * informacion adicional.
 */
public class ProcesoVencidoException extends RuntimeException {

    public ProcesoVencidoException(String reservationProcessId) {
        super("el proceso %s esta vencido: paso el plazo para responder".formatted(reservationProcessId));
    }

}

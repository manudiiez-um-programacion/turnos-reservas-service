package ar.edu.um.turnos.reservas.application.exception;

/**
 * El telefono, ya normalizado (quitando espacios, guiones, parentesis y
 * puntos), no cumple {@code ^\+?[0-9]{7,15}$} (#30, §15.5 de la referencia):
 * mismo criterio que valida la catedra, para no gastar un intento del
 * proceso con un valor que de todas formas va a rechazar.
 *
 * <p>El mensaje no repite el valor recibido a proposito: ni en logs ni en la
 * respuesta de error se escribe el telefono en claro.
 */
public class TelefonoInvalidoException extends RuntimeException {

    public TelefonoInvalidoException() {
        super("el telefono informado no es valido");
    }

}

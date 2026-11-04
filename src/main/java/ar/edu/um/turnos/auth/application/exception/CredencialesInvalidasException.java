package ar.edu.um.turnos.auth.application.exception;

/**
 * Login o contrasena incorrectos. Deliberadamente no distingue cual de los
 * dos fallo (#10, AC: "sin revelar si el usuario existe"): se lanza igual
 * si el login no existe, si el usuario no esta activo o si la contrasena no
 * coincide.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("usuario o contrasena incorrectos");
    }

}

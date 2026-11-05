package ar.edu.um.turnos.auth.application.exception;

/**
 * El {@code login} de un JWT valido ya no corresponde a ningun usuario
 * (#27): el token firma y vigencia correctas, pero el usuario se borro
 * despues de emitirse. Caso raro, pero distinto de unas credenciales
 * invalidas: aca no hay contrasena para comparar.
 */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(String login) {
        super("no existe un usuario con login '%s'".formatted(login));
    }

}

package ar.edu.um.turnos.auth.application.exception;

import lombok.Getter;

/**
 * El alta pidio un login o un email que ya tiene otro usuario. Se traduce a
 * un {@code 400 application/problem+json} en el borde web
 * ({@code RegistroExceptionHandler}), nunca a una excepcion generica: el
 * cliente necesita distinguir "login" de "email" para el mensaje al usuario.
 */
@Getter
public class UsuarioDuplicadoException extends RuntimeException {

    public enum Campo {
        LOGIN,
        EMAIL
    }

    private final Campo campo;

    public UsuarioDuplicadoException(Campo campo, String valor) {
        super("ya existe un usuario con %s '%s'".formatted(campo.name().toLowerCase(), valor));
        this.campo = campo;
    }

}

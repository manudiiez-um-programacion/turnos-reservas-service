package ar.edu.um.turnos.auth.infrastructure.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.turnos.auth.application.exception.UsuarioDuplicadoException;

/**
 * Traduce las excepciones del slice {@code auth} a
 * {@code application/problem+json}. Como pide CLAUDE.md §5 para los errores
 * de la catedra, el cliente tiene que decidir por {@code status} + un
 * codigo propio ({@code code}), nunca por el texto de {@code detail}.
 */
@RestControllerAdvice
public class RegistroExceptionHandler {

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ProblemDetail manejarUsuarioDuplicado(UsuarioDuplicadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Usuario duplicado");
        problema.setProperty("code",
                ex.getCampo() == UsuarioDuplicadoException.Campo.LOGIN ? "LOGIN_DUPLICADO" : "EMAIL_DUPLICADO");
        return problema;
    }

}

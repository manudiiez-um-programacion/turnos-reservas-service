package ar.edu.um.turnos.disponibilidad.infrastructure.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.disponibilidad.application.exception.RangoFechasInvalidoException;

/**
 * Traduce las excepciones del slice {@code disponibilidad} a
 * {@code application/problem+json} (#24). Como pide CLAUDE.md §5 para los
 * errores de la catedra y el catalogo, el cliente tiene que decidir por
 * {@code status} + {@code code}, nunca por el texto de {@code detail}.
 *
 * <p>Los {@code code} de {@code PROFESSIONAL_NOT_FOUND} y
 * {@code PROFESSIONAL_DISABLED} son deliberadamente los mismos que usa el
 * catalogo (#16): es la misma condicion de negocio, solo que expuesta a
 * traves de esta API en vez de la suya.
 */
@RestControllerAdvice
public class DisponibilidadExceptionHandler {

    @ExceptionHandler(ProfesionalNoEncontradoException.class)
    public ProblemDetail manejarProfesionalNoEncontrado(ProfesionalNoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Profesional inexistente");
        problema.setProperty("code", "PROFESSIONAL_NOT_FOUND");
        return problema;
    }

    @ExceptionHandler(ProfesionalDeshabilitadoException.class)
    public ProblemDetail manejarProfesionalDeshabilitado(ProfesionalDeshabilitadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Profesional deshabilitado");
        problema.setProperty("code", "PROFESSIONAL_DISABLED");
        return problema;
    }

    @ExceptionHandler(RangoFechasInvalidoException.class)
    public ProblemDetail manejarRangoFechasInvalido(RangoFechasInvalidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Rango de fechas invalido");
        problema.setProperty("code", "INVALID_DATE_RANGE");
        return problema;
    }

}

package ar.edu.um.turnos.reservas.infrastructure.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;

/**
 * Traduce las excepciones del slice {@code reservas} a
 * {@code application/problem+json} (#27). Como pide CLAUDE.md §5, el
 * cliente tiene que decidir por {@code status} + {@code code}, nunca por el
 * texto de {@code detail}.
 */
@RestControllerAdvice
public class ReservasExceptionHandler {

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

    @ExceptionHandler(SlotInvalidoException.class)
    public ProblemDetail manejarSlotInvalido(SlotInvalidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Slot invalido");
        problema.setProperty("code", "INVALID_SLOT");
        return problema;
    }

    @ExceptionHandler(SlotYaReservadoException.class)
    public ProblemDetail manejarSlotYaReservado(SlotYaReservadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Slot ya reservado");
        problema.setProperty("code", "SLOT_ALREADY_RESERVED");
        return problema;
    }

    @ExceptionHandler(SlotYaBloqueadoException.class)
    public ProblemDetail manejarSlotYaBloqueado(SlotYaBloqueadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Slot ya bloqueado");
        problema.setProperty("code", "SLOT_ALREADY_HELD");
        return problema;
    }

}

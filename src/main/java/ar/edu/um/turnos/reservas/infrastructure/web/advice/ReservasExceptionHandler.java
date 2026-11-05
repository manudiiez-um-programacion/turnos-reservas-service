package ar.edu.um.turnos.reservas.infrastructure.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoVencidoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.application.exception.TelefonoInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException;

/**
 * Traduce las excepciones del slice {@code reservas} a
 * {@code application/problem+json} (#27, #28). Como pide CLAUDE.md §5, el
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

    @ExceptionHandler(HoldNoEncontradoException.class)
    public ProblemDetail manejarHoldNoEncontrado(HoldNoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Hold inexistente");
        problema.setProperty("code", "HOLD_NOT_FOUND");
        return problema;
    }

    @ExceptionHandler(HoldVencidoException.class)
    public ProblemDetail manejarHoldVencido(HoldVencidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Hold vencido");
        problema.setProperty("code", "HOLD_EXPIRED");
        return problema;
    }

    @ExceptionHandler(HoldOwnerMismatchException.class)
    public ProblemDetail manejarHoldOwnerMismatch(HoldOwnerMismatchException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problema.setTitle("Hold de otra cuenta tecnica");
        problema.setProperty("code", "HOLD_OWNER_MISMATCH");
        return problema;
    }

    @ExceptionHandler(ExternalPatientMismatchException.class)
    public ProblemDetail manejarExternalPatientMismatch(ExternalPatientMismatchException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Paciente distinto al del hold");
        problema.setProperty("code", "EXTERNAL_PATIENT_MISMATCH");
        return problema;
    }

    @ExceptionHandler(ProcesoReservaAjenoException.class)
    public ProblemDetail manejarProcesoReservaAjeno(ProcesoReservaAjenoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problema.setTitle("Proceso de otro usuario");
        problema.setProperty("code", "RESERVATION_PROCESS_OWNER_MISMATCH");
        return problema;
    }

    @ExceptionHandler(TransicionProcesoReservaInvalidaException.class)
    public ProblemDetail manejarTransicionInvalida(TransicionProcesoReservaInvalidaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Proceso en un estado que no admite la operacion");
        problema.setProperty("code", "RESERVATION_PROCESS_INVALID_STATE");
        return problema;
    }

    @ExceptionHandler(TelefonoInvalidoException.class)
    public ProblemDetail manejarTelefonoInvalido(TelefonoInvalidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Telefono invalido");
        problema.setProperty("code", "INVALID_PHONE_NUMBER");
        return problema;
    }

    @ExceptionHandler(ProcesoVencidoException.class)
    public ProblemDetail manejarProcesoVencido(ProcesoVencidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Proceso vencido");
        problema.setProperty("code", "RESERVATION_PROCESS_EXPIRED");
        return problema;
    }

}

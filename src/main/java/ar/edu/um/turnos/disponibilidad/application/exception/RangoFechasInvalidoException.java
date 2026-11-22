package ar.edu.um.turnos.disponibilidad.application.exception;

import java.time.LocalDate;

/**
 * {@code from} posterior a {@code to} al pedir ocupaciones (#22). Se lanza
 * tanto en la validacion previa a llamar a la catedra como si, a pesar de
 * esa validacion, la catedra responde igual con {@code INVALID_DATE_RANGE}
 * (§13.1 de la referencia): las dos situaciones son el mismo error de
 * dominio.
 */
public class RangoFechasInvalidoException extends RuntimeException {

    public RangoFechasInvalidoException(LocalDate from, LocalDate to) {
        super("el rango de fechas es invalido: 'from' (%s) es posterior a 'to' (%s)".formatted(from, to));
    }

    public RangoFechasInvalidoException(LocalDate from, LocalDate to, Throwable causa) {
        super("el rango de fechas es invalido: 'from' (%s) es posterior a 'to' (%s)".formatted(from, to), causa);
    }

}

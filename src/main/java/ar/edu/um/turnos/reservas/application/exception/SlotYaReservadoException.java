package ar.edu.um.turnos.reservas.application.exception;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * La catedra informo {@code SLOT_ALREADY_RESERVED} al pedir el hold (#27):
 * el slot ya quedo confirmado por otra reserva. Comportamiento normal de
 * concurrencia, no un error del sistema.
 */
public class SlotYaReservadoException extends RuntimeException {

    public SlotYaReservadoException(Long professionalId, LocalDate fecha, LocalTime horaInicio, Throwable causa) {
        super("el slot %s %s del profesional %s ya esta reservado".formatted(fecha, horaInicio, professionalId), causa);
    }

}

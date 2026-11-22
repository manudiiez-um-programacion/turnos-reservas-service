package ar.edu.um.turnos.reservas.application.exception;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * La catedra informo {@code SLOT_ALREADY_HELD} al pedir el hold (#27): otro
 * proceso ya tiene un hold activo sobre el mismo slot. Comportamiento normal
 * de concurrencia (otro usuario llego primero), no un error del sistema.
 */
public class SlotYaBloqueadoException extends RuntimeException {

    public SlotYaBloqueadoException(Long professionalId, LocalDate fecha, LocalTime horaInicio, Throwable causa) {
        super("el slot %s %s del profesional %s ya esta bloqueado".formatted(fecha, horaInicio, professionalId), causa);
    }

}

package ar.edu.um.turnos.reservas.application.exception;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * La fecha y hora pedidas no corresponden a ningun slot habilitado de la
 * agenda vigente del profesional (#27): ya sea porque la validacion local
 * contra el catalogo lo detecto antes de llamar a la catedra, o porque la
 * catedra mismo informo {@code INVALID_SLOT} al pedir el hold (copia local
 * desactualizada entre la consulta y el pedido).
 */
public class SlotInvalidoException extends RuntimeException {

    public SlotInvalidoException(Long professionalId, LocalDate fecha, LocalTime horaInicio) {
        super(mensaje(professionalId, fecha, horaInicio));
    }

    public SlotInvalidoException(Long professionalId, LocalDate fecha, LocalTime horaInicio, Throwable causa) {
        super(mensaje(professionalId, fecha, horaInicio), causa);
    }

    private static String mensaje(Long professionalId, LocalDate fecha, LocalTime horaInicio) {
        return "el slot %s %s del profesional %s no es valido".formatted(fecha, horaInicio, professionalId);
    }

}

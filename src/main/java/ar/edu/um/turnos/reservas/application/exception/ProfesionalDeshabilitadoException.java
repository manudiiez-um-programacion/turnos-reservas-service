package ar.edu.um.turnos.reservas.application.exception;

/**
 * El catalogo informo {@code PROFESSIONAL_DISABLED} para el
 * {@code professionalId} pedido (#27): el dato existe, pero no se puede
 * iniciar una operacion nueva sobre un profesional deshabilitado.
 */
public class ProfesionalDeshabilitadoException extends RuntimeException {

    public ProfesionalDeshabilitadoException(Long professionalId, Throwable causa) {
        super("el profesional %s esta deshabilitado".formatted(professionalId), causa);
    }

}

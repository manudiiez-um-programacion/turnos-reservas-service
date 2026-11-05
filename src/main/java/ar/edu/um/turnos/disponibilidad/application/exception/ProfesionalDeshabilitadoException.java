package ar.edu.um.turnos.disponibilidad.application.exception;

/**
 * El catalogo informo {@code PROFESSIONAL_DISABLED} para el
 * {@code professionalId} pedido (#16, #24): el dato existe, pero no se
 * puede iniciar una operacion nueva sobre un profesional deshabilitado
 * (distinto de un {@link ProfesionalNoEncontradoException}, que es un dato
 * invalido).
 */
public class ProfesionalDeshabilitadoException extends RuntimeException {

    public ProfesionalDeshabilitadoException(Long professionalId, Throwable causa) {
        super("el profesional %s esta deshabilitado".formatted(professionalId), causa);
    }

}

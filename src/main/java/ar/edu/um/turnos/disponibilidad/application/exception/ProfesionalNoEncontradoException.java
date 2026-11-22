package ar.edu.um.turnos.disponibilidad.application.exception;

/**
 * El catalogo informo {@code PROFESSIONAL_NOT_FOUND} para el
 * {@code professionalId} pedido (#16, #24): es un dato invalido, no hay
 * nada que reintentar.
 */
public class ProfesionalNoEncontradoException extends RuntimeException {

    public ProfesionalNoEncontradoException(Long professionalId, Throwable causa) {
        super("no existe el profesional %s".formatted(professionalId), causa);
    }

}

package ar.edu.um.turnos.reservas.application.exception;

/**
 * El catalogo informo {@code PROFESSIONAL_NOT_FOUND} para el
 * {@code professionalId} pedido (#27): es un dato invalido, no hay nada que
 * reintentar.
 */
public class ProfesionalNoEncontradoException extends RuntimeException {

    public ProfesionalNoEncontradoException(Long professionalId, Throwable causa) {
        super("no existe el profesional %s".formatted(professionalId), causa);
    }

}

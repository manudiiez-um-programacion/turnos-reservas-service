package ar.edu.um.turnos.reservas.application.exception;

/**
 * La catedra informo {@code EXTERNAL_PATIENT_MISMATCH} al confirmar el hold
 * (#28, §10 de la referencia): el {@code externalPatientId} enviado no
 * coincide con el guardado en el hold. Es una inconsistencia que se
 * registra, no se reintenta.
 */
public class ExternalPatientMismatchException extends RuntimeException {

    public ExternalPatientMismatchException(String holdId, Throwable causa) {
        super("el externalPatientId no coincide con el guardado en el hold %s".formatted(holdId), causa);
    }

}

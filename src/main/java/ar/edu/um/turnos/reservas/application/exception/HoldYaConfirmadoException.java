package ar.edu.um.turnos.reservas.application.exception;

/**
 * La catedra informo {@code RESERVATION_PROCESS_ALREADY_CONFIRMED} al
 * confirmar el hold (#28, §10 de la referencia): la respuesta esperable
 * despues de un timeout en el que la catedra si procesó la confirmacion.
 * {@link ar.edu.um.turnos.reservas.application.usecases.ConfirmarHoldUseCaseImpl}
 * la captura y la trata como exito idempotente — tratarla como error nuevo
 * seria el bug que §18.4 advierte — por eso nunca deberia llegar a
 * {@link ar.edu.um.turnos.reservas.infrastructure.web.advice.ReservasExceptionHandler}.
 */
public class HoldYaConfirmadoException extends RuntimeException {

    public HoldYaConfirmadoException(String holdId, Throwable causa) {
        super("el proceso del hold %s ya estaba confirmado inicialmente".formatted(holdId), causa);
    }

}

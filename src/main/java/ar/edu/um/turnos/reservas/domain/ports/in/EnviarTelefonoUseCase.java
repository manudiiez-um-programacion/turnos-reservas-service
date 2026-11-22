package ar.edu.um.turnos.reservas.domain.ports.in;

import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;

/**
 * Puerto de entrada: responde {@code AdditionalInformationRequested}
 * enviando el telefono del usuario (#30, §15.5 de la referencia y punto 11
 * del alcance obligatorio del enunciado).
 */
public interface EnviarTelefonoUseCase {

    /**
     * @throws ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException
     *         si el {@code holdId} no corresponde a ningun proceso local.
     * @throws ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException
     *         si el usuario autenticado no es el dueño del proceso.
     * @throws ar.edu.um.turnos.reservas.application.exception.TelefonoInvalidoException
     *         si el telefono, ya normalizado, no cumple {@code ^\+?[0-9]{7,15}$}
     *         (mismo criterio que la catedra, §15.5): se rechaza antes de
     *         publicar, para no gastar un intento del proceso.
     * @throws ar.edu.um.turnos.reservas.application.exception.ProcesoVencidoException
     *         si {@code expiresAt} ya paso: el proceso se cierra como
     *         {@link ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva#VENCIDA}
     *         y no se publica nada.
     * @throws ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException
     *         si el proceso no esta esperando el telefono ni fue rechazado
     *         (por ejemplo, ya esta confirmado, vencido o invalido).
     */
    ProcesoReserva enviar(SolicitudEnvioTelefono solicitud);

}

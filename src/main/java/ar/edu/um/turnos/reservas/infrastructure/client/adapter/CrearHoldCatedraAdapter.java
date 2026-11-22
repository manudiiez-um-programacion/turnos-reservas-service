package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.CrearHoldRequest;
import ar.edu.um.turnos.config.catedra.dto.HoldResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.domain.model.HoldCreado;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.domain.ports.out.CrearHoldCatedraPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: pide el hold a {@link CatedraClient} (#27,
 * §9 de la referencia) y traduce los errores funcionales de la matriz de
 * §13.1 a las excepciones propias de este slice. Un error que esa matriz no
 * documenta para este endpoint se deja propagar sin traducir.
 */
@Component
@RequiredArgsConstructor
public class CrearHoldCatedraAdapter implements CrearHoldCatedraPort {

    private final CatedraClient catedraClient;

    @Override
    public HoldCreado crear(SolicitudHold solicitud) {
        CrearHoldRequest request = new CrearHoldRequest(
                solicitud.professionalId(), solicitud.fecha(), solicitud.horaInicio(), solicitud.externalPatientId());
        try {
            HoldResponse respuesta = catedraClient.crearHold(request);
            return new HoldCreado(respuesta.holdId(), respuesta.reservationProcessId(), respuesta.expiresAt());
        } catch (CatedraClientErrorException error) {
            throw traducirError(solicitud, error);
        }
    }

    private RuntimeException traducirError(SolicitudHold solicitud, CatedraClientErrorException error) {
        return switch (error.getErrorCode()) {
            case PROFESSIONAL_NOT_FOUND -> new ProfesionalNoEncontradoException(solicitud.professionalId(), error);
            case PROFESSIONAL_DISABLED -> new ProfesionalDeshabilitadoException(solicitud.professionalId(), error);
            case INVALID_SLOT ->
                    new SlotInvalidoException(solicitud.professionalId(), solicitud.fecha(), solicitud.horaInicio(), error);
            case SLOT_ALREADY_RESERVED ->
                    new SlotYaReservadoException(solicitud.professionalId(), solicitud.fecha(), solicitud.horaInicio(), error);
            case SLOT_ALREADY_HELD ->
                    new SlotYaBloqueadoException(solicitud.professionalId(), solicitud.fecha(), solicitud.horaInicio(), error);
            default -> error;
        };
    }

}

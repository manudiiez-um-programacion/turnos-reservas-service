package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.ConfirmacionHoldResponse;
import ar.edu.um.turnos.config.catedra.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.HoldYaConfirmadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.domain.model.ConfirmacionHoldCatedra;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ConfirmarHoldCatedraPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: confirma el hold en {@link CatedraClient}
 * (#28, §10 de la referencia) y traduce los errores funcionales de la
 * matriz de §13.1 a las excepciones propias de este slice. Un error que esa
 * matriz no documenta para este endpoint se deja propagar sin traducir.
 */
@Component
@RequiredArgsConstructor
public class ConfirmarHoldCatedraAdapter implements ConfirmarHoldCatedraPort {

    private final CatedraClient catedraClient;

    @Override
    public ConfirmacionHoldCatedra confirmar(
            ProcesoReserva proceso, String externalPatientId, String patientFirstName, String patientLastName) {
        ConfirmarHoldRequest request = new ConfirmarHoldRequest(externalPatientId, patientFirstName, patientLastName);
        try {
            ConfirmacionHoldResponse respuesta = catedraClient.confirmarHold(proceso.getHoldId(), request);
            return new ConfirmacionHoldCatedra(respuesta.reservationProcessId(), respuesta.status(), respuesta.expiresAt());
        } catch (CatedraClientErrorException error) {
            throw traducirError(proceso, error);
        }
    }

    private RuntimeException traducirError(ProcesoReserva proceso, CatedraClientErrorException error) {
        return switch (error.getErrorCode()) {
            case PROFESSIONAL_NOT_FOUND -> new ProfesionalNoEncontradoException(proceso.getProfessionalId(), error);
            case INVALID_SLOT ->
                    new SlotInvalidoException(proceso.getProfessionalId(), proceso.getFecha(), proceso.getHoraInicio(), error);
            case SLOT_ALREADY_RESERVED ->
                    new SlotYaReservadoException(proceso.getProfessionalId(), proceso.getFecha(), proceso.getHoraInicio(), error);
            case HOLD_NOT_FOUND -> new HoldNoEncontradoException(proceso.getHoldId(), error);
            case HOLD_EXPIRED -> new HoldVencidoException(proceso.getHoldId(), error);
            case HOLD_OWNER_MISMATCH -> new HoldOwnerMismatchException(proceso.getHoldId(), error);
            case EXTERNAL_PATIENT_MISMATCH -> new ExternalPatientMismatchException(proceso.getHoldId(), error);
            case RESERVATION_PROCESS_ALREADY_CONFIRMED -> new HoldYaConfirmadoException(proceso.getHoldId(), error);
            default -> error;
        };
    }

}

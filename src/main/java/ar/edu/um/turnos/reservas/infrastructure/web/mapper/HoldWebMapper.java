package ar.edu.um.turnos.reservas.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.CrearHoldRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.EnviarTelefonoRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.HoldResponse;

/** Traduce entre el borde web de holds (#27, #28) y el dominio de {@code reservas}. */
@Component
public class HoldWebMapper {

    public SolicitudHold toSolicitud(CrearHoldRequest request, Usuario usuario) {
        if (request == null) {
            return null;
        }
        return SolicitudHold.builder()
                .professionalId(request.professionalId())
                .fecha(request.date())
                .horaInicio(request.startTime())
                .usuarioId(usuario.getId())
                .externalPatientId(usuario.getExternalPatientId())
                .build();
    }

    public SolicitudConfirmacionHold toSolicitudConfirmacion(String holdId, ConfirmarHoldRequest request, Usuario usuario) {
        if (request == null) {
            return null;
        }
        return SolicitudConfirmacionHold.builder()
                .holdId(holdId)
                .usuarioId(usuario.getId())
                .externalPatientId(usuario.getExternalPatientId())
                .patientFirstName(request.patientFirstName())
                .patientLastName(request.patientLastName())
                .build();
    }

    public SolicitudEnvioTelefono toSolicitudEnvioTelefono(String holdId, EnviarTelefonoRequest request, Usuario usuario) {
        if (request == null) {
            return null;
        }
        return SolicitudEnvioTelefono.builder()
                .holdId(holdId)
                .usuarioId(usuario.getId())
                .telefono(request.phoneNumber())
                .build();
    }

    public HoldResponse toResponse(ProcesoReserva proceso) {
        if (proceso == null) {
            return null;
        }
        return new HoldResponse(
                proceso.getHoldId(),
                proceso.getReservationProcessId(),
                proceso.getEstado() == null ? null : proceso.getEstado().name(),
                proceso.getExpiresAt());
    }

}

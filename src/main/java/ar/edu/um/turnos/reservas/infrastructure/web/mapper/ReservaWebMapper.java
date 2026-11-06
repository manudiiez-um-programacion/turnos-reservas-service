package ar.edu.um.turnos.reservas.infrastructure.web.mapper;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.application.exception.EstadoReservaInvalidoException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudCancelacionReserva;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.CancelarReservaRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ReservaResponse;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ReservasPaginaResponse;

/** Traduce entre el borde web de {@code GET /api/reservas} (#33) y el dominio de {@code reservas}. */
@Component
public class ReservaWebMapper {

    public FiltroReservasUsuario toFiltro(
            Long usuarioId, String status, LocalDate from, LocalDate to, int page, int size) {
        return FiltroReservasUsuario.builder()
                .usuarioId(usuarioId)
                .estado(toEstado(status))
                .desde(from)
                .hasta(to)
                .pagina(page)
                .tamanio(size)
                .build();
    }

    public ReservasPaginaResponse toPaginaResponse(PaginaProcesoReserva pagina, int page, int size) {
        if (pagina == null) {
            return null;
        }
        List<ReservaResponse> content = pagina.contenido().stream().map(this::toResponse).toList();
        return new ReservasPaginaResponse(content, pagina.totalElementos(), page, size);
    }

    public ReservaResponse toResponse(ProcesoReserva proceso) {
        if (proceso == null) {
            return null;
        }
        return new ReservaResponse(
                proceso.getHoldId(),
                proceso.getReservationProcessId(),
                proceso.getEstado() == null ? null : proceso.getEstado().name(),
                proceso.getProfessionalId(),
                proceso.getProfessionalNombreHistorico(),
                proceso.getProfessionalApellidoHistorico(),
                proceso.getFecha(),
                proceso.getHoraInicio(),
                proceso.getExpiresAt(),
                proceso.getReservationId(),
                proceso.getReason(),
                proceso.getReasonMessage(),
                proceso.getCancelledAt(),
                proceso.getCancelReason());
    }

    public SolicitudCancelacionReserva toSolicitudCancelacion(String reservationProcessId, CancelarReservaRequest request, Usuario usuario) {
        return SolicitudCancelacionReserva.builder()
                .reservationProcessId(reservationProcessId)
                .usuarioId(usuario.getId())
                .reason(request == null ? null : request.reason())
                .build();
    }

    private EstadoProcesoReserva toEstado(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EstadoProcesoReserva.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new EstadoReservaInvalidoException(status);
        }
    }

}

package ar.edu.um.turnos.reservas.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.TransicionProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.TransicionProcesoReservaEmbeddable;

/** Traduce entre {@link ProcesoReserva} (dominio) y {@link ProcesoReservaJpaEntity} (JPA). */
@Component
public class ProcesoReservaPersistenceMapper {

    public ProcesoReservaJpaEntity toEntity(ProcesoReserva proceso) {
        if (proceso == null) {
            return null;
        }
        return ProcesoReservaJpaEntity.builder()
                .id(proceso.getId())
                .reservationProcessId(proceso.getReservationProcessId())
                .holdId(proceso.getHoldId())
                .usuarioId(proceso.getUsuarioId())
                .professionalId(proceso.getProfessionalId())
                .professionalNombreHistorico(proceso.getProfessionalNombreHistorico())
                .professionalApellidoHistorico(proceso.getProfessionalApellidoHistorico())
                .fecha(proceso.getFecha())
                .horaInicio(proceso.getHoraInicio())
                .estado(proceso.getEstado() == null ? null : proceso.getEstado().name())
                .expiresAt(proceso.getExpiresAt())
                .requestEventId(proceso.getRequestEventId())
                .requestRequiredField(proceso.getRequestRequiredField())
                .requestMessage(proceso.getRequestMessage())
                .telefono(proceso.getTelefono())
                .reservationId(proceso.getReservationId())
                .reason(proceso.getReason())
                .reasonMessage(proceso.getReasonMessage())
                .intentosTelefono(proceso.getIntentosTelefono())
                .historial(toHistorialEmbeddable(proceso.getHistorial()))
                .build();
    }

    public ProcesoReserva toDomain(ProcesoReservaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcesoReserva.builder()
                .id(entity.getId())
                .reservationProcessId(entity.getReservationProcessId())
                .holdId(entity.getHoldId())
                .usuarioId(entity.getUsuarioId())
                .professionalId(entity.getProfessionalId())
                .professionalNombreHistorico(entity.getProfessionalNombreHistorico())
                .professionalApellidoHistorico(entity.getProfessionalApellidoHistorico())
                .fecha(entity.getFecha())
                .horaInicio(entity.getHoraInicio())
                .estado(entity.getEstado() == null ? null : EstadoProcesoReserva.valueOf(entity.getEstado()))
                .expiresAt(entity.getExpiresAt())
                .requestEventId(entity.getRequestEventId())
                .requestRequiredField(entity.getRequestRequiredField())
                .requestMessage(entity.getRequestMessage())
                .telefono(entity.getTelefono())
                .reservationId(entity.getReservationId())
                .reason(entity.getReason())
                .reasonMessage(entity.getReasonMessage())
                .intentosTelefono(entity.getIntentosTelefono())
                .historial(toHistorialDomain(entity.getHistorial()))
                .build();
    }

    private List<TransicionProcesoReservaEmbeddable> toHistorialEmbeddable(List<TransicionProcesoReserva> historial) {
        List<TransicionProcesoReservaEmbeddable> resultado = new ArrayList<>();
        if (historial == null) {
            return resultado;
        }
        for (TransicionProcesoReserva transicion : historial) {
            resultado.add(new TransicionProcesoReservaEmbeddable(transicion.estado().name(), transicion.ocurridoEn()));
        }
        return resultado;
    }

    private List<TransicionProcesoReserva> toHistorialDomain(List<TransicionProcesoReservaEmbeddable> historial) {
        List<TransicionProcesoReserva> resultado = new ArrayList<>();
        if (historial == null) {
            return resultado;
        }
        for (TransicionProcesoReservaEmbeddable transicion : historial) {
            resultado.add(new TransicionProcesoReserva(
                    EstadoProcesoReserva.valueOf(transicion.getEstado()), transicion.getOcurridoEn()));
        }
        return resultado;
    }

}

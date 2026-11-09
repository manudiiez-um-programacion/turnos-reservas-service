package ar.edu.um.turnos.reservas.infrastructure.persistence.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;

public interface ProcesoReservaJpaRepository
        extends JpaRepository<ProcesoReservaJpaEntity, Long>, JpaSpecificationExecutor<ProcesoReservaJpaEntity> {

    Optional<ProcesoReservaJpaEntity> findByReservationProcessId(String reservationProcessId);

    Optional<ProcesoReservaJpaEntity> findByHoldId(String holdId);

    Optional<ProcesoReservaJpaEntity> findFirstByUsuarioIdAndProfessionalIdAndFechaAndHoraInicioAndEstadoNotInAndExpiresAtAfter(
            Long usuarioId, Long professionalId, LocalDate fecha, LocalTime horaInicio,
            Collection<String> estadosExcluidos, Instant ahora);

    List<ProcesoReservaJpaEntity> findByEstadoNotIn(Collection<String> estadosExcluidos);

}

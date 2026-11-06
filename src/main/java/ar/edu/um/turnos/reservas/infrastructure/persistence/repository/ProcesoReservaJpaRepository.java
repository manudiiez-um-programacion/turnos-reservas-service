package ar.edu.um.turnos.reservas.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;

public interface ProcesoReservaJpaRepository
        extends JpaRepository<ProcesoReservaJpaEntity, Long>, JpaSpecificationExecutor<ProcesoReservaJpaEntity> {

    Optional<ProcesoReservaJpaEntity> findByReservationProcessId(String reservationProcessId);

    Optional<ProcesoReservaJpaEntity> findByHoldId(String holdId);

}

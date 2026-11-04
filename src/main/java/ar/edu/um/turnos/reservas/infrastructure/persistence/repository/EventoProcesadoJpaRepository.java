package ar.edu.um.turnos.reservas.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.EventoProcesadoJpaEntity;

public interface EventoProcesadoJpaRepository extends JpaRepository<EventoProcesadoJpaEntity, String> {

}

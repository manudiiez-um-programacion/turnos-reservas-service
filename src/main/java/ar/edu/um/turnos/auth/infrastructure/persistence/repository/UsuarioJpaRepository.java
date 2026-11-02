package ar.edu.um.turnos.auth.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.turnos.auth.infrastructure.persistence.entity.UsuarioJpaEntity;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

}

package ar.edu.um.turnos.auth.infrastructure.persistence.mapper;

import java.util.HashSet;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.infrastructure.persistence.entity.UsuarioJpaEntity;

/** Traduce entre {@link Usuario} (dominio) y {@link UsuarioJpaEntity} (JPA). */
@Component
public class UsuarioPersistenceMapper {

    public UsuarioJpaEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioJpaEntity.builder()
                .id(usuario.getId())
                .externalPatientId(usuario.getExternalPatientId())
                .login(usuario.getLogin())
                .passwordHash(usuario.getPasswordHash())
                .firstName(usuario.getFirstName())
                .lastName(usuario.getLastName())
                .email(usuario.getEmail())
                .imageUrl(usuario.getImageUrl())
                .langKey(usuario.getLangKey())
                .activated(usuario.isActivated())
                .authorities(usuario.getAuthorities() == null ? new HashSet<>() : new HashSet<>(usuario.getAuthorities()))
                .build();
    }

    public Usuario toDomain(UsuarioJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Usuario.builder()
                .id(entity.getId())
                .externalPatientId(entity.getExternalPatientId())
                .login(entity.getLogin())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .imageUrl(entity.getImageUrl())
                .langKey(entity.getLangKey())
                .activated(entity.isActivated())
                .authorities(entity.getAuthorities() == null ? new HashSet<>() : new HashSet<>(entity.getAuthorities()))
                .build();
    }

}

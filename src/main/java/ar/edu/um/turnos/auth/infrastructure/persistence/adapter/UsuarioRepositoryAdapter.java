package ar.edu.um.turnos.auth.infrastructure.persistence.adapter;

import java.util.Optional;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;
import ar.edu.um.turnos.auth.infrastructure.persistence.mapper.UsuarioPersistenceMapper;
import ar.edu.um.turnos.auth.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public boolean existeConLogin(String login) {
        return usuarioJpaRepository.existsByLogin(login);
    }

    @Override
    public boolean existeConEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        return usuarioJpaRepository.findByLogin(login).map(mapper::toDomain);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return mapper.toDomain(usuarioJpaRepository.save(mapper.toEntity(usuario)));
    }

}

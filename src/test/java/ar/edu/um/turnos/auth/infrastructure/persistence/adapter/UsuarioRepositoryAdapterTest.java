package ar.edu.um.turnos.auth.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.infrastructure.persistence.entity.UsuarioJpaEntity;
import ar.edu.um.turnos.auth.infrastructure.persistence.mapper.UsuarioPersistenceMapper;
import ar.edu.um.turnos.auth.infrastructure.persistence.repository.UsuarioJpaRepository;

/** Verifica que el adaptador solo delega en el repositorio JPA y en el mapper, sin logica propia. */
@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryAdapterTest {

    @Mock
    private UsuarioJpaRepository usuarioJpaRepository;

    private final UsuarioPersistenceMapper mapper = new UsuarioPersistenceMapper();

    private UsuarioRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioRepositoryAdapter(usuarioJpaRepository, mapper);
    }

    @Test
    void delegaExisteConLoginEnElRepositorio() {
        when(usuarioJpaRepository.existsByLogin("ana.gomez")).thenReturn(true);

        assertThat(adapter.existeConLogin("ana.gomez")).isTrue();
    }

    @Test
    void delegaExisteConEmailEnElRepositorio() {
        when(usuarioJpaRepository.existsByEmail("ana@ejemplo.com")).thenReturn(false);

        assertThat(adapter.existeConEmail("ana@ejemplo.com")).isFalse();
    }

    @Test
    void guardaMapeandoDeDominioAEntidadYDeVueltaADominio() {
        Usuario usuario = Usuario.builder().login("ana.gomez").email("ana@ejemplo.com").build();
        UsuarioJpaEntity entidadGuardada = UsuarioJpaEntity.builder().id(9L).login("ana.gomez").email("ana@ejemplo.com").build();
        when(usuarioJpaRepository.save(any(UsuarioJpaEntity.class))).thenReturn(entidadGuardada);

        Usuario resultado = adapter.guardar(usuario);

        assertThat(resultado.getId()).isEqualTo(9L);
        verify(usuarioJpaRepository).save(any(UsuarioJpaEntity.class));
    }

}

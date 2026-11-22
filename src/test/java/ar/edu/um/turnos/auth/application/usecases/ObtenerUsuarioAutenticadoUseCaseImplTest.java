package ar.edu.um.turnos.auth.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.auth.application.exception.UsuarioNoEncontradoException;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ObtenerUsuarioAutenticadoUseCaseImplTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private ObtenerUsuarioAutenticadoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ObtenerUsuarioAutenticadoUseCaseImpl(usuarioRepositoryPort);
    }

    @Test
    void devuelveElUsuarioCuandoElLoginExiste() {
        Usuario esperado = Usuario.builder().login("ana.gomez").build();
        when(usuarioRepositoryPort.buscarPorLogin("ana.gomez")).thenReturn(Optional.of(esperado));

        assertThat(useCase.obtenerPorLogin("ana.gomez")).isSameAs(esperado);
    }

    @Test
    void rechazaUnLoginQueYaNoExiste() {
        when(usuarioRepositoryPort.buscarPorLogin("no.existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.obtenerPorLogin("no.existe"))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

}

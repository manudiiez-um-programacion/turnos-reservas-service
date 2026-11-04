package ar.edu.um.turnos.auth.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.auth.application.exception.CredencialesInvalidasException;
import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.out.JwtIssuerPort;
import ar.edu.um.turnos.auth.domain.ports.out.PasswordHasherPort;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioUseCaseImplTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private JwtIssuerPort jwtIssuerPort;

    private AutenticarUsuarioUseCaseImpl useCase;

    private Usuario usuarioActivo;

    @BeforeEach
    void setUp() {
        useCase = new AutenticarUsuarioUseCaseImpl(usuarioRepositoryPort, passwordHasherPort, jwtIssuerPort);
        usuarioActivo = Usuario.builder()
                .login("ana.gomez")
                .passwordHash("{argon2}hash")
                .activated(true)
                .build();
    }

    private SolicitudAutenticacion solicitud(String login, String password) {
        return SolicitudAutenticacion.builder().login(login).password(password).build();
    }

    @Test
    void autenticaYDevuelveElTokenCuandoLasCredencialesSonCorrectas() {
        when(usuarioRepositoryPort.buscarPorLogin("ana.gomez")).thenReturn(Optional.of(usuarioActivo));
        when(passwordHasherPort.verificar("secreta", "{argon2}hash")).thenReturn(true);
        when(jwtIssuerPort.emitir(usuarioActivo)).thenReturn("un-jwt");

        String token = useCase.autenticar(solicitud("Ana.Gomez", "secreta"));

        assertThat(token).isEqualTo("un-jwt");
        verify(usuarioRepositoryPort).buscarPorLogin("ana.gomez");
    }

    @Test
    void rechazaUnLoginQueNoExisteSinRevelarlo() {
        when(usuarioRepositoryPort.buscarPorLogin("no.existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.autenticar(solicitud("no.existe", "cualquiera")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(passwordHasherPort, never()).verificar(any(), any());
        verify(jwtIssuerPort, never()).emitir(any());
    }

    @Test
    void rechazaUnUsuarioDesactivadoComoSiNoExistiera() {
        Usuario desactivado = Usuario.builder().login("ana.gomez").passwordHash("{argon2}hash").activated(false).build();
        when(usuarioRepositoryPort.buscarPorLogin("ana.gomez")).thenReturn(Optional.of(desactivado));

        assertThatThrownBy(() -> useCase.autenticar(solicitud("ana.gomez", "secreta")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(passwordHasherPort, never()).verificar(any(), any());
    }

    @Test
    void rechazaUnaContraseniaIncorrecta() {
        when(usuarioRepositoryPort.buscarPorLogin("ana.gomez")).thenReturn(Optional.of(usuarioActivo));
        when(passwordHasherPort.verificar("incorrecta", "{argon2}hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.autenticar(solicitud("ana.gomez", "incorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(jwtIssuerPort, never()).emitir(any());
    }

    @Test
    void rechazaUnaSolicitudNulaConUnaExcepcionClara() {
        assertThatThrownBy(() -> useCase.autenticar(null)).isInstanceOf(NullPointerException.class);
    }

}

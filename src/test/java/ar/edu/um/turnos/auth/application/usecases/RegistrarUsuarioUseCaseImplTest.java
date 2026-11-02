package ar.edu.um.turnos.auth.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.auth.application.exception.UsuarioDuplicadoException;
import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.out.PasswordHasherPort;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioUseCaseImplTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    private RegistrarUsuarioUseCaseImpl useCase;

    private SolicitudRegistroUsuario solicitud;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarUsuarioUseCaseImpl(usuarioRepositoryPort, passwordHasherPort);
        solicitud = SolicitudRegistroUsuario.builder()
                .login("Nueva.Paciente")
                .password("una-contrasenia-cualquiera")
                .firstName("Ana")
                .lastName("Gomez")
                .email("Ana.Gomez@Ejemplo.com")
                .langKey("es")
                .build();
    }

    @Test
    void registraConLoginYEmailNormalizadosAMinusculas() {
        when(usuarioRepositoryPort.existeConLogin(anyString())).thenReturn(false);
        when(usuarioRepositoryPort.existeConEmail(anyString())).thenReturn(false);
        when(passwordHasherPort.hashear("una-contrasenia-cualquiera")).thenReturn("{argon2}hash-simulado");
        when(usuarioRepositoryPort.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario registrado = useCase.registrar(solicitud);

        assertThat(registrado.getLogin()).isEqualTo("nueva.paciente");
        assertThat(registrado.getEmail()).isEqualTo("ana.gomez@ejemplo.com");
        assertThat(registrado.getPasswordHash()).isEqualTo("{argon2}hash-simulado");
        assertThat(registrado.isActivated()).isTrue();
        assertThat(registrado.getAuthorities()).containsExactly("ROLE_USER");
        assertThat(registrado.getExternalPatientId()).isNotBlank();

        verify(usuarioRepositoryPort).existeConLogin("nueva.paciente");
        verify(usuarioRepositoryPort).existeConEmail("ana.gomez@ejemplo.com");
    }

    @Test
    void generaUnExternalPatientIdDistintoEnCadaAlta() {
        when(passwordHasherPort.hashear(anyString())).thenReturn("{argon2}hash");
        when(usuarioRepositoryPort.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario primero = useCase.registrar(solicitud);
        Usuario segundo = useCase.registrar(solicitud);

        assertThat(primero.getExternalPatientId()).isNotEqualTo(segundo.getExternalPatientId());
    }

    @Test
    void rechazaUnLoginYaUsado() {
        when(usuarioRepositoryPort.existeConLogin("nueva.paciente")).thenReturn(true);

        assertThatThrownBy(() -> useCase.registrar(solicitud))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .extracting(ex -> ((UsuarioDuplicadoException) ex).getCampo())
                .isEqualTo(UsuarioDuplicadoException.Campo.LOGIN);

        verify(usuarioRepositoryPort, org.mockito.Mockito.never()).guardar(any());
        verify(usuarioRepositoryPort, org.mockito.Mockito.never()).existeConEmail(anyString());
    }

    @Test
    void rechazaUnaSolicitudNulaConUnaExcepcionClara() {
        assertThatThrownBy(() -> useCase.registrar(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rechazaUnEmailYaUsado() {
        when(usuarioRepositoryPort.existeConLogin(anyString())).thenReturn(false);
        when(usuarioRepositoryPort.existeConEmail("ana.gomez@ejemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.registrar(solicitud))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .extracting(ex -> ((UsuarioDuplicadoException) ex).getCampo())
                .isEqualTo(UsuarioDuplicadoException.Campo.EMAIL);

        verify(usuarioRepositoryPort, org.mockito.Mockito.never()).guardar(any());
    }

}

package ar.edu.um.turnos.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;
import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.AutenticarUsuarioUseCase;
import ar.edu.um.turnos.auth.domain.ports.in.RegistrarUsuarioUseCase;

/** La fachada no tiene logica propia: solo delega en el caso de uso que corresponda. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @Mock
    private AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    @Test
    void delegaElRegistroEnElCasoDeUso() {
        SolicitudRegistroUsuario solicitud = SolicitudRegistroUsuario.builder().login("ana.gomez").build();
        Usuario esperado = Usuario.builder().login("ana.gomez").build();
        when(registrarUsuarioUseCase.registrar(solicitud)).thenReturn(esperado);

        AuthService service = new AuthService(registrarUsuarioUseCase, autenticarUsuarioUseCase);

        assertThat(service.registrar(solicitud)).isSameAs(esperado);
    }

    @Test
    void delegaLaAutenticacionEnElCasoDeUso() {
        SolicitudAutenticacion solicitud = SolicitudAutenticacion.builder().login("ana.gomez").password("secreta").build();
        when(autenticarUsuarioUseCase.autenticar(solicitud)).thenReturn("un-jwt");

        AuthService service = new AuthService(registrarUsuarioUseCase, autenticarUsuarioUseCase);

        assertThat(service.autenticar(solicitud)).isEqualTo("un-jwt");
    }

}

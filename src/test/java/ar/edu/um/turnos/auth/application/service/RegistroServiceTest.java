package ar.edu.um.turnos.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.RegistrarUsuarioUseCase;

/** La fachada no tiene logica propia: solo delega en el caso de uso. */
@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @Test
    void delegaElRegistroEnElCasoDeUso() {
        SolicitudRegistroUsuario solicitud = SolicitudRegistroUsuario.builder().login("ana.gomez").build();
        Usuario esperado = Usuario.builder().login("ana.gomez").build();
        when(registrarUsuarioUseCase.registrar(solicitud)).thenReturn(esperado);

        RegistroService service = new RegistroService(registrarUsuarioUseCase);

        assertThat(service.registrar(solicitud)).isSameAs(esperado);
    }

}

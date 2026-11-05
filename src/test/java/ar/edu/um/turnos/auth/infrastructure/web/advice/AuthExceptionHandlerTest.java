package ar.edu.um.turnos.auth.infrastructure.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import ar.edu.um.turnos.auth.application.exception.UsuarioNoEncontradoException;

@DisplayName("AuthExceptionHandler")
class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    @DisplayName("UsuarioNoEncontradoException se traduce a un 401 con code USUARIO_NO_ENCONTRADO")
    void usuarioNoEncontradoSeTraduceA401() {
        ProblemDetail problema = handler.manejarUsuarioNoEncontrado(new UsuarioNoEncontradoException("ana.gomez"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problema.getProperties()).containsEntry("code", "USUARIO_NO_ENCONTRADO");
    }

}

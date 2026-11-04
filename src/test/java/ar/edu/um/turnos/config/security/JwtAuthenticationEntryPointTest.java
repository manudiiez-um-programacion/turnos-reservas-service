package ar.edu.um.turnos.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Verifica el {@code 401} que exige el AC de #11 ("un request sin token
 * devuelve 401") y que el detalle nunca repite el mensaje interno de la
 * excepcion de Spring Security, que podria describir por que fallo el JWT.
 */
class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @Test
    void respondeNoAutorizadoEnProblemJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new BadCredentialsException("firma invalida: no revelar esto al cliente"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("\"code\":\"NO_AUTENTICADO\"")
                .doesNotContain("firma invalida");
    }

}

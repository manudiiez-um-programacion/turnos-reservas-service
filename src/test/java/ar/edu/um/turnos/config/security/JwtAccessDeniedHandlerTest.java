package ar.edu.um.turnos.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

/**
 * Verifica el {@code 403} que exige el AC de #11 ("con token valido pero sin
 * permisos, 403") y que el detalle no filtra el mensaje interno.
 */
class JwtAccessDeniedHandlerTest {

    private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler();

    @Test
    void respondeAccesoDenegadoEnProblemJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("detalle interno que no se debe exponer"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"status\":403")
                .contains("\"code\":\"ACCESO_DENEGADO\"")
                .doesNotContain("detalle interno que no se debe exponer");
    }

}

package ar.edu.um.turnos.auth.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.auth.application.exception.CredencialesInvalidasException;
import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.infrastructure.web.advice.AuthExceptionHandler;
import ar.edu.um.turnos.auth.infrastructure.web.mapper.AutenticacionWebMapper;

/**
 * Prueba solo el borde web: validacion, el header Authorization, el cuerpo
 * id_token y la traduccion de credenciales invalidas a 401 problem+json. Sin
 * filtro de seguridad: la validacion de JWT en los endpoints llega en #11.
 */
@WebMvcTest(controllers = AutenticacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AutenticacionWebMapper.class, AuthExceptionHandler.class})
class AutenticacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String REQUEST_VALIDO = """
            {
              "username": "ana.gomez",
              "password": "una-contrasenia"
            }
            """;

    @Test
    void autenticaYDevuelveElTokenEnElHeaderYEnElCuerpo() throws Exception {
        when(authService.autenticar(any())).thenReturn("un-jwt");

        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer un-jwt"))
                .andExpect(jsonPath("$.id_token").value("un-jwt"));
    }

    @Test
    void rechazaCredencialesInvalidasCon401YUnCodigoGenerico() throws Exception {
        when(authService.autenticar(any())).thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void rechazaUnUsernameEnBlancoCon400() throws Exception {
        String request = """
                {
                  "username": "",
                  "password": "una-contrasenia"
                }
                """;

        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnaContraseniaDemasiadoCortaCon400() throws Exception {
        String request = """
                {
                  "username": "ana.gomez",
                  "password": "abc"
                }
                """;

        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

}

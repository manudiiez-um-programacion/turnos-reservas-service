package ar.edu.um.turnos.auth.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import ar.edu.um.turnos.auth.application.exception.UsuarioDuplicadoException;
import ar.edu.um.turnos.auth.application.service.RegistroService;
import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.infrastructure.web.advice.RegistroExceptionHandler;
import ar.edu.um.turnos.auth.infrastructure.web.mapper.RegistroWebMapper;

/**
 * Prueba solo el borde web: validacion, mapeo y traduccion de excepciones a
 * problem+json. No hay base real en esta etapa (Testcontainers llega en
 * #41), asi que la fachada se mockea y el filtro de seguridad se apaga: la
 * seguridad definitiva de #11 no es lo que se verifica aca.
 */
@WebMvcTest(controllers = RegistroController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RegistroWebMapper.class, RegistroExceptionHandler.class})
class RegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroService registroService;

    private static final String REQUEST_VALIDO = """
            {
              "login": "nueva.paciente",
              "password": "una-contrasenia",
              "firstName": "Ana",
              "lastName": "Gomez",
              "email": "ana@ejemplo.com",
              "langKey": "es"
            }
            """;

    @Test
    void registraYDevuelve201SinCuerpo() throws Exception {
        when(registroService.registrar(any())).thenReturn(Usuario.builder().build());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").doesNotExist());

        verify(registroService).registrar(any(SolicitudRegistroUsuario.class));
    }

    @Test
    void rechazaUnLoginEnBlancoCon400() throws Exception {
        String request = """
                {
                  "login": "",
                  "password": "una-contrasenia",
                  "email": "ana@ejemplo.com",
                  "langKey": "es"
                }
                """;

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnaContraseniaDemasiadoCortaCon400() throws Exception {
        String request = """
                {
                  "login": "nueva.paciente",
                  "password": "abc",
                  "email": "ana@ejemplo.com",
                  "langKey": "es"
                }
                """;

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelveProblemJsonConCodeCuandoElLoginYaExiste() throws Exception {
        when(registroService.registrar(any()))
                .thenThrow(new UsuarioDuplicadoException(UsuarioDuplicadoException.Campo.LOGIN, "nueva.paciente"));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_DUPLICADO"));
    }

}

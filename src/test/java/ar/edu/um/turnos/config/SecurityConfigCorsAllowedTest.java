package ar.edu.um.turnos.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.health.domain.model.EstadoDependencia;
import ar.edu.um.turnos.health.domain.model.EstadoDependencias;
import ar.edu.um.turnos.health.domain.ports.out.EstadoDependenciasPort;

/**
 * Con un origen listado en {@code turnos.cors.allowed-origins}, el preflight
 * y el pedido real tienen que pasar y traer
 * {@code Access-Control-Allow-Origin}. Contexto aparte del de
 * {@link SecurityConfigTest} porque la propiedad cambia entre pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "turnos.cors.allowed-origins=https://app.permitido.test")
@DisplayName("SecurityConfig con un origen CORS permitido")
class SecurityConfigCorsAllowedTest {

    private static final String ORIGEN_PERMITIDO = "https://app.permitido.test";

    @Autowired
    private MockMvc mockMvc;

    // #40: ver el mismo mock en SecurityConfigTest. Esta clase prueba CORS,
    // no el estado de salud.
    @MockitoBean
    private EstadoDependenciasPort estadoDependenciasPort;

    @BeforeEach
    void estadoDependenciasDisponible() {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DESCONOCIDO)
                .catalogo(EstadoDependencia.DESCONOCIDO)
                .build());
    }

    @Test
    @DisplayName("el preflight de un endpoint protegido pasa sin token")
    void elPreflightPasaSinToken() throws Exception {
        mockMvc.perform(options("/api/una-ruta-que-no-existe")
                        .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO));
    }

    @Test
    @DisplayName("un pedido real desde el origen permitido trae el header de CORS")
    void unPedidoRealTraeElHeader() throws Exception {
        mockMvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO));
    }

    @Test
    @DisplayName("un origen que no esta en la lista sigue sin header, aunque haya otros permitidos")
    void unOrigenNoListadoNoTraeElHeader() throws Exception {
        mockMvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, "https://otro-no-permitido.test"))
                .andExpect(status().isForbidden());
    }

}

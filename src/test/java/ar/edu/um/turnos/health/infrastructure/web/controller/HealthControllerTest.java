package ar.edu.um.turnos.health.infrastructure.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Recorre el slice de punta a punta: HTTP -> controlador -> fachada -> caso de
 * uso -> puerto de salida -> adaptador, y la vuelta mapeada a DTO.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devuelveElEstadoDelServicio() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.nombre").value("turnos-reservas-service"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.consultadoEn").exists());
    }

    @Test
    void elEndpointEsPublicoEnEsteMilestone() throws Exception {
        // Documenta el estado provisional de SecurityConfig: en #11 este endpoint
        // sigue siendo publico, pero el resto de la API deja de serlo.
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

}

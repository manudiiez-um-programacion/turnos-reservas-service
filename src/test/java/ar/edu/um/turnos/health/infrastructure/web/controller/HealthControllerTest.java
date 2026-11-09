package ar.edu.um.turnos.health.infrastructure.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.health.domain.model.EstadoDependencia;
import ar.edu.um.turnos.health.domain.model.EstadoDependencias;
import ar.edu.um.turnos.health.domain.ports.out.EstadoDependenciasPort;

/**
 * Recorre el slice de punta a punta: HTTP -> controlador -> fachada -> caso de
 * uso -> puerto de salida -> adaptador, y la vuelta mapeada a DTO.
 *
 * <p>{@link EstadoDependenciasPort} se mockea (#40): el adaptador real abre
 * una conexion JDBC de verdad, y en el perfil {@code test} no hay base
 * levantada (ver application-test.properties). Mockear el puerto, no el
 * adaptador, es ademas lo que corresponde en hexagonal: la prueba HTTP no
 * tiene por que saber que la implementacion real usa un DataSource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstadoDependenciasPort estadoDependenciasPort;

    @Test
    void devuelveElEstadoDelServicio() throws Exception {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DESCONOCIDO)
                .catalogo(EstadoDependencia.DESCONOCIDO)
                .build());

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.nombre").value("turnos-reservas-service"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.consultadoEn").exists())
                .andExpect(jsonPath("$.dependencias.baseDeDatos").value("DISPONIBLE"))
                .andExpect(jsonPath("$.dependencias.catedra").value("DESCONOCIDO"))
                .andExpect(jsonPath("$.dependencias.catalogo").value("DESCONOCIDO"));
    }

    @Test
    void conLaBaseArribaYLaCatedraCaidaElServicioQuedaDegradadoPeroResponde200() throws Exception {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DEGRADADO)
                .catalogo(EstadoDependencia.DISPONIBLE)
                .build());

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEGRADADO"));
    }

    @Test
    void conLaBaseCaidaElServicioQuedaDownYResponde503() throws Exception {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DEGRADADO)
                .catedra(EstadoDependencia.DISPONIBLE)
                .catalogo(EstadoDependencia.DISPONIBLE)
                .build());

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.estado").value("DOWN"));
    }

    @Test
    void elEndpointEsPublico() throws Exception {
        // SecurityConfig (#11) lo trata como infraestructura, no como negocio: el
        // healthcheck de compose.yaml lo consulta sin credenciales.
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DESCONOCIDO)
                .catalogo(EstadoDependencia.DESCONOCIDO)
                .build());

        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

}

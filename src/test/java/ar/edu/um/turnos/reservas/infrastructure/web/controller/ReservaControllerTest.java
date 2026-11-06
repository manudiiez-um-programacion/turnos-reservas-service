package ar.edu.um.turnos.reservas.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.web.advice.ReservasExceptionHandler;
import ar.edu.um.turnos.reservas.infrastructure.web.mapper.ReservaWebMapper;

/**
 * Prueba solo el borde web, igual criterio que {@link HoldControllerTest}:
 * sin filtro de seguridad, el principal se inyecta a mano.
 */
@WebMvcTest(controllers = ReservaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ReservaWebMapper.class, ReservasExceptionHandler.class})
class ReservaControllerTest {

    private static final Authentication AUTENTICACION = new TestingAuthenticationToken("ana.gomez", null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservasService reservasService;

    @MockitoBean
    private AuthService authService;

    @Test
    void listaLasReservasDelUsuarioAutenticadoYDevuelve200() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.CONFIRMADA)
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .build();
        when(reservasService.consultarReservas(any())).thenReturn(new PaginaProcesoReserva(List.of(proceso), 1L));

        mockMvc.perform(get("/api/reservas").principal(AUTENTICACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].holdId").value("hold-6c0c2f41"))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void aceptaLosFiltrosDeStatusYRangoDeFechas() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.consultarReservas(any())).thenReturn(new PaginaProcesoReserva(List.of(), 0L));

        mockMvc.perform(get("/api/reservas")
                        .param("status", "CONFIRMADA")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .param("page", "1")
                        .param("size", "5")
                        .principal(AUTENTICACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void rechazaUnStatusInvalidoCon400() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());

        mockMvc.perform(get("/api/reservas").param("status", "NO_EXISTE").principal(AUTENTICACION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    void nuncaExponeLasReservasDeOtroUsuario() throws Exception {
        Usuario usuario = Usuario.builder().id(7L).externalPatientId("user-42").build();
        when(authService.obtenerPorLogin("ana.gomez")).thenReturn(usuario);
        when(reservasService.consultarReservas(any())).thenAnswer(invocation -> {
            FiltroReservasUsuario filtro = invocation.getArgument(0, FiltroReservasUsuario.class);
            assertThat(filtro.usuarioId()).isEqualTo(7L);
            return new PaginaProcesoReserva(List.of(), 0L);
        });

        mockMvc.perform(get("/api/reservas").principal(AUTENTICACION)).andExpect(status().isOk());
    }

}

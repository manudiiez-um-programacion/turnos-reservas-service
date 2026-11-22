package ar.edu.um.turnos.disponibilidad.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.disponibilidad.application.service.DisponibilidadService;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.advice.DisponibilidadExceptionHandler;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.mapper.DisponibilidadWebMapper;

/**
 * Prueba solo el borde web: el binding de los query params, el formato de
 * la respuesta y la traduccion de errores funcionales a problem+json. Sin
 * filtro de seguridad: eso ya lo cubre #11 a nivel global.
 */
@WebMvcTest(controllers = DisponibilidadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DisponibilidadWebMapper.class, DisponibilidadExceptionHandler.class})
class DisponibilidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisponibilidadService disponibilidadService;

    @Test
    void devuelveLosTurnosDisponiblesParaLaFechaYElProfesionalPedidos() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 9, 14);
        List<Slot> slots = List.of(new Slot(fecha, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        when(disponibilidadService.obtenerTurnosDisponibles(eq(15L), eq(fecha))).thenReturn(slots);

        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalId").value(15))
                .andExpect(jsonPath("$.date").value("2026-09-14"))
                .andExpect(jsonPath("$.availableSlots[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.availableSlots[0].endTime").value("09:30:00"));
    }

    @Test
    void devuelveUnaListaVaciaCuandoNoHayTurnosLibres() throws Exception {
        when(disponibilidadService.obtenerTurnosDisponibles(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").isEmpty());
    }

    @Test
    void rechazaUnProfesionalInexistenteCon404() throws Exception {
        when(disponibilidadService.obtenerTurnosDisponibles(any(), any()))
                .thenThrow(new ProfesionalNoEncontradoException(15L, null));

        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "2026-09-14"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"));
    }

    @Test
    void rechazaUnProfesionalDeshabilitadoCon409() throws Exception {
        when(disponibilidadService.obtenerTurnosDisponibles(any(), any()))
                .thenThrow(new ProfesionalDeshabilitadoException(15L, null));

        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "2026-09-14"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_DISABLED"));
    }

    @Test
    void rechazaUnaFechaConFormatoInvalidoCon400() throws Exception {
        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "14-09-2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaLaFaltaDelProfessionalIdCon400() throws Exception {
        mockMvc.perform(get("/api/disponibilidad")
                        .param("date", "2026-09-14"))
                .andExpect(status().isBadRequest());
    }

}

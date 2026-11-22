package ar.edu.um.turnos.reservas.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoVencidoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.application.exception.TelefonoInvalidoException;
import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.web.advice.ReservasExceptionHandler;
import ar.edu.um.turnos.reservas.infrastructure.web.mapper.HoldWebMapper;

/**
 * Prueba solo el borde web: el binding del cuerpo, la resolucion del
 * usuario autenticado y la traduccion de errores funcionales a
 * problem+json. Sin filtro de seguridad (eso ya lo cubre #11 a nivel
 * global): el principal se inyecta a mano con
 * {@code MockHttpServletRequestBuilder#principal}, que fija
 * {@code HttpServletRequest#getUserPrincipal()} directo, sin depender del
 * filtro que normalmente lo completa a partir del {@code SecurityContext}.
 */
@WebMvcTest(controllers = HoldController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({HoldWebMapper.class, ReservasExceptionHandler.class})
class HoldControllerTest {

    private static final Authentication AUTENTICACION = new TestingAuthenticationToken("ana.gomez", null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservasService reservasService;

    @MockitoBean
    private AuthService authService;

    private String cuerpoRequest() {
        return """
                {"professionalId": 15, "date": "2026-09-14", "startTime": "09:30"}
                """;
    }

    @Test
    void creaElHoldParaElUsuarioAutenticadoYDevuelve201() throws Exception {
        Usuario usuario = Usuario.builder().id(7L).externalPatientId("user-42").build();
        when(authService.obtenerPorLogin("ana.gomez")).thenReturn(usuario);
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.HOLD_CREADO)
                .expiresAt(expiresAt)
                .build();
        when(reservasService.crearHold(any())).thenReturn(proceso);

        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdId").value("hold-6c0c2f41"))
                .andExpect(jsonPath("$.reservationProcessId").value("process-a92b47d0"))
                .andExpect(jsonPath("$.status").value("HOLD_CREADO"));
    }

    @Test
    void rechazaUnCuerpoSinProfessionalIdCon400() throws Exception {
        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2026-09-14", "startTime": "09:30"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnProfesionalInexistenteCon404() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.crearHold(any())).thenThrow(new ProfesionalNoEncontradoException(15L, null));

        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"));
    }

    @Test
    void rechazaUnProfesionalDeshabilitadoCon409() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.crearHold(any())).thenThrow(new ProfesionalDeshabilitadoException(15L, null));

        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_DISABLED"));
    }

    @Test
    void rechazaUnSlotInvalidoCon400() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.crearHold(any()))
                .thenThrow(new SlotInvalidoException(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30)));

        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SLOT"));
    }

    @Test
    void rechazaUnSlotYaReservadoCon409() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.crearHold(any()))
                .thenThrow(new SlotYaReservadoException(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), null));

        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_ALREADY_RESERVED"));
    }

    @Test
    void rechazaUnSlotYaBloqueadoCon409() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.crearHold(any()))
                .thenThrow(new SlotYaBloqueadoException(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), null));

        mockMvc.perform(post("/api/holds")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_ALREADY_HELD"));
    }

    private String cuerpoRequestConfirmacion() {
        return """
                {"patientFirstName": "Juan", "patientLastName": "Perez"}
                """;
    }

    @Test
    void confirmaElHoldParaElUsuarioAutenticadoYDevuelve202() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.ESPERANDO_TELEFONO)
                .expiresAt(expiresAt)
                .build();
        when(reservasService.confirmarHold(any())).thenReturn(proceso);

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/confirm")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestConfirmacion()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationProcessId").value("process-a92b47d0"))
                .andExpect(jsonPath("$.status").value("ESPERANDO_TELEFONO"));
    }

    @Test
    void rechazaUnCuerpoDeConfirmacionSinApellidoCon400() throws Exception {
        mockMvc.perform(post("/api/holds/hold-6c0c2f41/confirm")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientFirstName": "Juan"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnHoldInexistenteCon404() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.confirmarHold(any())).thenThrow(new HoldNoEncontradoException("hold-6c0c2f41"));

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/confirm")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestConfirmacion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));
    }

    @Test
    void rechazaUnHoldVencidoCon404() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.confirmarHold(any())).thenThrow(new HoldVencidoException("hold-6c0c2f41", null));

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/confirm")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestConfirmacion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOLD_EXPIRED"));
    }

    @Test
    void rechazaConfirmarElProcesoDeOtroUsuarioCon403() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.confirmarHold(any())).thenThrow(new ProcesoReservaAjenoException("hold-6c0c2f41", 7L));

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/confirm")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestConfirmacion()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_OWNER_MISMATCH"));
    }

    private String cuerpoRequestTelefono() {
        return """
                {"phoneNumber": "+54 9 261-555-1234"}
                """;
    }

    @Test
    void enviaElTelefonoParaElUsuarioAutenticadoYDevuelve202() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.TELEFONO_ENVIADO)
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .build();
        when(reservasService.enviarTelefono(any())).thenReturn(proceso);

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/phone")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestTelefono()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationProcessId").value("process-a92b47d0"))
                .andExpect(jsonPath("$.status").value("TELEFONO_ENVIADO"));
    }

    @Test
    void rechazaUnCuerpoDeTelefonoSinPhoneNumberCon400() throws Exception {
        mockMvc.perform(post("/api/holds/hold-6c0c2f41/phone")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnTelefonoInvalidoCon400() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.enviarTelefono(any())).thenThrow(new TelefonoInvalidoException());

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/phone")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestTelefono()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE_NUMBER"));
    }

    @Test
    void rechazaUnProcesoVencidoCon404() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.enviarTelefono(any())).thenThrow(new ProcesoVencidoException("process-a92b47d0"));

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/phone")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestTelefono()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_EXPIRED"));
    }

    @Test
    void rechazaEnviarElTelefonoDeOtroUsuarioCon403() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.enviarTelefono(any())).thenThrow(new ProcesoReservaAjenoException("hold-6c0c2f41", 7L));

        mockMvc.perform(post("/api/holds/hold-6c0c2f41/phone")
                        .principal(AUTENTICACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRequestTelefono()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_OWNER_MISMATCH"));
    }

    @Test
    void consultaElEstadoDelHoldParaElUsuarioAutenticadoYDevuelve200() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.ESPERANDO_TELEFONO)
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .build();
        when(reservasService.consultarHold(eq("hold-6c0c2f41"), eq(7L))).thenReturn(proceso);

        mockMvc.perform(get("/api/holds/hold-6c0c2f41").principal(AUTENTICACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationProcessId").value("process-a92b47d0"))
                .andExpect(jsonPath("$.status").value("ESPERANDO_TELEFONO"));
    }

    @Test
    void rechazaConsultarUnHoldInexistenteCon404() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.consultarHold(eq("hold-6c0c2f41"), eq(7L)))
                .thenThrow(new HoldNoEncontradoException("hold-6c0c2f41"));

        mockMvc.perform(get("/api/holds/hold-6c0c2f41").principal(AUTENTICACION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));
    }

    @Test
    void rechazaConsultarElProcesoDeOtroUsuarioCon403() throws Exception {
        when(authService.obtenerPorLogin("ana.gomez"))
                .thenReturn(Usuario.builder().id(7L).externalPatientId("user-42").build());
        when(reservasService.consultarHold(eq("hold-6c0c2f41"), eq(7L)))
                .thenThrow(new ProcesoReservaAjenoException("hold-6c0c2f41", 7L));

        mockMvc.perform(get("/api/holds/hold-6c0c2f41").principal(AUTENTICACION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_PROCESS_OWNER_MISMATCH"));
    }

}

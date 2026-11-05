package ar.edu.um.turnos.reservas.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.CrearHoldRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.EnviarTelefonoRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.HoldResponse;

class HoldWebMapperTest {

    private final HoldWebMapper mapper = new HoldWebMapper();

    @Test
    void traduceElRequestYElUsuarioAutenticadoALaSolicitudDeDominio() {
        CrearHoldRequest request = new CrearHoldRequest(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30));
        Usuario usuario = Usuario.builder().id(7L).externalPatientId("user-42").build();

        SolicitudHold solicitud = mapper.toSolicitud(request, usuario);

        assertThat(solicitud).isEqualTo(SolicitudHold.builder()
                .professionalId(15L)
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .usuarioId(7L)
                .externalPatientId("user-42")
                .build());
    }

    @Test
    void traduceElProcesoDeDominioALaRespuesta() {
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.HOLD_CREADO)
                .expiresAt(expiresAt)
                .build();

        HoldResponse respuesta = mapper.toResponse(proceso);

        assertThat(respuesta)
                .isEqualTo(new HoldResponse("hold-6c0c2f41", "process-a92b47d0", "HOLD_CREADO", expiresAt, null, null, null));
    }

    @Test
    void traduceElResultadoFinalDeDominioALaRespuesta() {
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.CONFIRMADA)
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .reservationId(8251L)
                .reason("HOLD_EXPIRED")
                .reasonMessage("algun mensaje")
                .build();

        HoldResponse respuesta = mapper.toResponse(proceso);

        assertThat(respuesta.reservationId()).isEqualTo(8251L);
        assertThat(respuesta.reason()).isEqualTo("HOLD_EXPIRED");
        assertThat(respuesta.reasonMessage()).isEqualTo("algun mensaje");
    }

    @Test
    void traduceElRequestDeConfirmacionYElUsuarioAutenticadoALaSolicitudDeDominio() {
        ConfirmarHoldRequest request = new ConfirmarHoldRequest("Juan", "Perez");
        Usuario usuario = Usuario.builder().id(7L).externalPatientId("user-42").build();

        SolicitudConfirmacionHold solicitud = mapper.toSolicitudConfirmacion("hold-6c0c2f41", request, usuario);

        assertThat(solicitud).isEqualTo(SolicitudConfirmacionHold.builder()
                .holdId("hold-6c0c2f41")
                .usuarioId(7L)
                .externalPatientId("user-42")
                .patientFirstName("Juan")
                .patientLastName("Perez")
                .build());
    }

    @Test
    void traduceElRequestDeTelefonoYElUsuarioAutenticadoALaSolicitudDeDominio() {
        EnviarTelefonoRequest request = new EnviarTelefonoRequest("+54 9 261-555-1234");
        Usuario usuario = Usuario.builder().id(7L).externalPatientId("user-42").build();

        SolicitudEnvioTelefono solicitud = mapper.toSolicitudEnvioTelefono("hold-6c0c2f41", request, usuario);

        assertThat(solicitud).isEqualTo(SolicitudEnvioTelefono.builder()
                .holdId("hold-6c0c2f41")
                .usuarioId(7L)
                .telefono("+54 9 261-555-1234")
                .build());
    }

}

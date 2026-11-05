package ar.edu.um.turnos.reservas.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.CrearHoldRequest;
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

        assertThat(respuesta).isEqualTo(new HoldResponse("hold-6c0c2f41", "process-a92b47d0", "HOLD_CREADO", expiresAt));
    }

}

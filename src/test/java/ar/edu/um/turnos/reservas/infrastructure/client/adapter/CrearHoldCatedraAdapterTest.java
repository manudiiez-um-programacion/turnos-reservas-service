package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.CrearHoldRequest;
import ar.edu.um.turnos.config.catedra.dto.HoldResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.domain.model.HoldCreado;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;

@ExtendWith(MockitoExtension.class)
class CrearHoldCatedraAdapterTest {

    private final SolicitudHold solicitud = SolicitudHold.builder()
            .professionalId(15L)
            .fecha(LocalDate.of(2026, 9, 14))
            .horaInicio(LocalTime.of(9, 30))
            .usuarioId(7L)
            .externalPatientId("user-42")
            .build();

    private final CrearHoldRequest requestEsperado =
            new CrearHoldRequest(15L, solicitud.fecha(), solicitud.horaInicio(), "user-42");

    @Mock
    private CatedraClient catedraClient;

    private CrearHoldCatedraAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CrearHoldCatedraAdapter(catedraClient);
    }

    @Test
    void delegaEnCatedraClientYTraduceLaRespuesta() {
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        when(catedraClient.crearHold(requestEsperado))
                .thenReturn(new HoldResponse("hold-6c0c2f41", "process-a92b47d0", "HELD", expiresAt));

        HoldCreado resultado = adapter.crear(solicitud);

        assertThat(resultado).isEqualTo(new HoldCreado("hold-6c0c2f41", "process-a92b47d0", expiresAt));
    }

    @Test
    void traducePROFESSIONAL_NOT_FOUND() {
        when(catedraClient.crearHold(requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.crear(solicitud)).isInstanceOf(ProfesionalNoEncontradoException.class);
    }

    @Test
    void traducePROFESSIONAL_DISABLED() {
        when(catedraClient.crearHold(requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.BAD_REQUEST, "PROFESSIONAL_DISABLED", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.crear(solicitud)).isInstanceOf(ProfesionalDeshabilitadoException.class);
    }

    @Test
    void traduceINVALID_SLOT() {
        when(catedraClient.crearHold(requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.BAD_REQUEST, "INVALID_SLOT", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.crear(solicitud)).isInstanceOf(SlotInvalidoException.class);
    }

    @Test
    void traduceSLOT_ALREADY_RESERVED() {
        when(catedraClient.crearHold(requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.CONFLICT, "SLOT_ALREADY_RESERVED", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.crear(solicitud)).isInstanceOf(SlotYaReservadoException.class);
    }

    @Test
    void traduceSLOT_ALREADY_HELD() {
        when(catedraClient.crearHold(requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.CONFLICT, "SLOT_ALREADY_HELD", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.crear(solicitud)).isInstanceOf(SlotYaBloqueadoException.class);
    }

    @Test
    void noTraduceOtrosErroresFuncionales() {
        CatedraClientErrorException original =
                new CatedraClientErrorException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "detalle", List.of());
        when(catedraClient.crearHold(requestEsperado)).thenThrow(original);

        assertThatThrownBy(() -> adapter.crear(solicitud)).isSameAs(original);
    }

}

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
import ar.edu.um.turnos.config.catedra.dto.ConfirmacionHoldResponse;
import ar.edu.um.turnos.config.catedra.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.HoldYaConfirmadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.domain.model.ConfirmacionHoldCatedra;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

@ExtendWith(MockitoExtension.class)
class ConfirmarHoldCatedraAdapterTest {

    private final ProcesoReserva proceso = ProcesoReserva.builder()
            .holdId("hold-6c0c2f41")
            .reservationProcessId("process-a92b47d0")
            .professionalId(15L)
            .fecha(LocalDate.of(2026, 9, 14))
            .horaInicio(LocalTime.of(9, 30))
            .build();

    private final ConfirmarHoldRequest requestEsperado = new ConfirmarHoldRequest("user-42", "Juan", "Perez");

    @Mock
    private CatedraClient catedraClient;

    private ConfirmarHoldCatedraAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ConfirmarHoldCatedraAdapter(catedraClient);
    }

    @Test
    void delegaEnCatedraClientYTraduceLaRespuesta() {
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenReturn(new ConfirmacionHoldResponse("process-a92b47d0", "WAITING_FOR_PHONE", expiresAt));

        ConfirmacionHoldCatedra resultado = adapter.confirmar(proceso, "user-42", "Juan", "Perez");

        assertThat(resultado).isEqualTo(new ConfirmacionHoldCatedra("process-a92b47d0", "WAITING_FOR_PHONE", expiresAt));
    }

    @Test
    void traducePROFESSIONAL_NOT_FOUND() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(ProfesionalNoEncontradoException.class);
    }

    @Test
    void traduceINVALID_SLOT() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.BAD_REQUEST, "INVALID_SLOT", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(SlotInvalidoException.class);
    }

    @Test
    void traduceSLOT_ALREADY_RESERVED() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.CONFLICT, "SLOT_ALREADY_RESERVED", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(SlotYaReservadoException.class);
    }

    @Test
    void traduceHOLD_NOT_FOUND() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.NOT_FOUND, "HOLD_NOT_FOUND", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(HoldNoEncontradoException.class);
    }

    @Test
    void traduceHOLD_EXPIRED() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.NOT_FOUND, "HOLD_EXPIRED", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(HoldVencidoException.class);
    }

    @Test
    void traduceHOLD_OWNER_MISMATCH() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(HttpStatus.FORBIDDEN, "HOLD_OWNER_MISMATCH", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(HoldOwnerMismatchException.class);
    }

    @Test
    void traduceEXTERNAL_PATIENT_MISMATCH() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(
                        HttpStatus.BAD_REQUEST, "EXTERNAL_PATIENT_MISMATCH", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(ExternalPatientMismatchException.class);
    }

    @Test
    void traduceRESERVATION_PROCESS_ALREADY_CONFIRMED() {
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado))
                .thenThrow(new CatedraClientErrorException(
                        HttpStatus.CONFLICT, "RESERVATION_PROCESS_ALREADY_CONFIRMED", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez"))
                .isInstanceOf(HoldYaConfirmadoException.class);
    }

    @Test
    void noTraduceOtrosErroresFuncionales() {
        CatedraClientErrorException original =
                new CatedraClientErrorException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "detalle", List.of());
        when(catedraClient.confirmarHold("hold-6c0c2f41", requestEsperado)).thenThrow(original);

        assertThatThrownBy(() -> adapter.confirmar(proceso, "user-42", "Juan", "Perez")).isSameAs(original);
    }

}

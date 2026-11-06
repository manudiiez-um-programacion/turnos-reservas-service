package ar.edu.um.turnos.reservas.infrastructure.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import ar.edu.um.turnos.reservas.application.exception.EstadoReservaInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.LimiteReintentosTelefonoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoVencidoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException;
import ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;
import ar.edu.um.turnos.reservas.application.exception.TelefonoInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;

@DisplayName("ReservasExceptionHandler")
class ReservasExceptionHandlerTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 14);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private final ReservasExceptionHandler handler = new ReservasExceptionHandler();

    @Test
    @DisplayName("ProfesionalNoEncontradoException se traduce a un 404 con code PROFESSIONAL_NOT_FOUND")
    void profesionalNoEncontradoSeTraduceA404() {
        ProblemDetail problema = handler.manejarProfesionalNoEncontrado(new ProfesionalNoEncontradoException(15L, null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getProperties()).containsEntry("code", "PROFESSIONAL_NOT_FOUND");
    }

    @Test
    @DisplayName("ProfesionalDeshabilitadoException se traduce a un 409 con code PROFESSIONAL_DISABLED")
    void profesionalDeshabilitadoSeTraduceA409() {
        ProblemDetail problema = handler.manejarProfesionalDeshabilitado(new ProfesionalDeshabilitadoException(15L, null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getProperties()).containsEntry("code", "PROFESSIONAL_DISABLED");
    }

    @Test
    @DisplayName("SlotInvalidoException se traduce a un 400 con code INVALID_SLOT")
    void slotInvalidoSeTraduceA400() {
        ProblemDetail problema = handler.manejarSlotInvalido(new SlotInvalidoException(15L, FECHA, HORA));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getProperties()).containsEntry("code", "INVALID_SLOT");
    }

    @Test
    @DisplayName("SlotYaReservadoException se traduce a un 409 con code SLOT_ALREADY_RESERVED")
    void slotYaReservadoSeTraduceA409() {
        ProblemDetail problema = handler.manejarSlotYaReservado(new SlotYaReservadoException(15L, FECHA, HORA, null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getProperties()).containsEntry("code", "SLOT_ALREADY_RESERVED");
    }

    @Test
    @DisplayName("SlotYaBloqueadoException se traduce a un 409 con code SLOT_ALREADY_HELD")
    void slotYaBloqueadoSeTraduceA409() {
        ProblemDetail problema = handler.manejarSlotYaBloqueado(new SlotYaBloqueadoException(15L, FECHA, HORA, null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getProperties()).containsEntry("code", "SLOT_ALREADY_HELD");
    }

    @Test
    @DisplayName("HoldNoEncontradoException se traduce a un 404 con code HOLD_NOT_FOUND")
    void holdNoEncontradoSeTraduceA404() {
        ProblemDetail problema = handler.manejarHoldNoEncontrado(new HoldNoEncontradoException("hold-1"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getProperties()).containsEntry("code", "HOLD_NOT_FOUND");
    }

    @Test
    @DisplayName("HoldVencidoException se traduce a un 404 con code HOLD_EXPIRED")
    void holdVencidoSeTraduceA404() {
        ProblemDetail problema = handler.manejarHoldVencido(new HoldVencidoException("hold-1", null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getProperties()).containsEntry("code", "HOLD_EXPIRED");
    }

    @Test
    @DisplayName("HoldOwnerMismatchException se traduce a un 403 con code HOLD_OWNER_MISMATCH")
    void holdOwnerMismatchSeTraduceA403() {
        ProblemDetail problema = handler.manejarHoldOwnerMismatch(new HoldOwnerMismatchException("hold-1", null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problema.getProperties()).containsEntry("code", "HOLD_OWNER_MISMATCH");
    }

    @Test
    @DisplayName("ExternalPatientMismatchException se traduce a un 400 con code EXTERNAL_PATIENT_MISMATCH")
    void externalPatientMismatchSeTraduceA400() {
        ProblemDetail problema = handler.manejarExternalPatientMismatch(new ExternalPatientMismatchException("hold-1", null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getProperties()).containsEntry("code", "EXTERNAL_PATIENT_MISMATCH");
    }

    @Test
    @DisplayName("ProcesoReservaAjenoException se traduce a un 403 con code RESERVATION_PROCESS_OWNER_MISMATCH")
    void procesoReservaAjenoSeTraduceA403() {
        ProblemDetail problema = handler.manejarProcesoReservaAjeno(new ProcesoReservaAjenoException("hold-1", 7L));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problema.getProperties()).containsEntry("code", "RESERVATION_PROCESS_OWNER_MISMATCH");
    }

    @Test
    @DisplayName("TransicionProcesoReservaInvalidaException se traduce a un 409 con code RESERVATION_PROCESS_INVALID_STATE")
    void transicionInvalidaSeTraduceA409() {
        ProblemDetail problema = handler.manejarTransicionInvalida(new TransicionProcesoReservaInvalidaException(
                "process-1", EstadoProcesoReserva.CONFIRMADA, EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getProperties()).containsEntry("code", "RESERVATION_PROCESS_INVALID_STATE");
    }

    @Test
    @DisplayName("TelefonoInvalidoException se traduce a un 400 con code INVALID_PHONE_NUMBER")
    void telefonoInvalidoSeTraduceA400() {
        ProblemDetail problema = handler.manejarTelefonoInvalido(new TelefonoInvalidoException());

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getProperties()).containsEntry("code", "INVALID_PHONE_NUMBER");
    }

    @Test
    @DisplayName("ProcesoVencidoException se traduce a un 404 con code RESERVATION_PROCESS_EXPIRED")
    void procesoVencidoSeTraduceA404() {
        ProblemDetail problema = handler.manejarProcesoVencido(new ProcesoVencidoException("process-1"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getProperties()).containsEntry("code", "RESERVATION_PROCESS_EXPIRED");
    }

    @Test
    @DisplayName("LimiteReintentosTelefonoException se traduce a un 409 con code PHONE_RETRY_LIMIT_EXCEEDED")
    void limiteReintentosTelefonoSeTraduceA409() {
        ProblemDetail problema =
                handler.manejarLimiteReintentosTelefono(new LimiteReintentosTelefonoException("process-1", 3));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getProperties()).containsEntry("code", "PHONE_RETRY_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("EstadoReservaInvalidoException se traduce a un 400 con code INVALID_STATUS")
    void estadoReservaInvalidoSeTraduceA400() {
        ProblemDetail problema = handler.manejarEstadoReservaInvalido(new EstadoReservaInvalidoException("NO_EXISTE"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getProperties()).containsEntry("code", "INVALID_STATUS");
    }

    @Test
    @DisplayName("ReservaNoEncontradaException se traduce a un 404 con code RESERVATION_NOT_FOUND")
    void reservaNoEncontradaSeTraduceA404() {
        ProblemDetail problema = handler.manejarReservaNoEncontrada(new ReservaNoEncontradaException("process-1"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getProperties()).containsEntry("code", "RESERVATION_NOT_FOUND");
    }

    @Test
    @DisplayName("ReservaOwnerMismatchException se traduce a un 403 con code RESERVATION_OWNER_MISMATCH")
    void reservaOwnerMismatchSeTraduceA403() {
        ProblemDetail problema = handler.manejarReservaOwnerMismatch(new ReservaOwnerMismatchException("process-1", null));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problema.getProperties()).containsEntry("code", "RESERVATION_OWNER_MISMATCH");
    }

    @Test
    @DisplayName("ReservaNoCancelableException se traduce a un 409 con code RESERVATION_NOT_CANCELLABLE")
    void reservaNoCancelableSeTraduceA409() {
        ProblemDetail problema = handler.manejarReservaNoCancelable(new ReservaNoCancelableException("process-1"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getProperties()).containsEntry("code", "RESERVATION_NOT_CANCELLABLE");
    }

}

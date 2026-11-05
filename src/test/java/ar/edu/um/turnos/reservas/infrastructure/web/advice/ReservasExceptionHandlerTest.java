package ar.edu.um.turnos.reservas.infrastructure.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaBloqueadoException;
import ar.edu.um.turnos.reservas.application.exception.SlotYaReservadoException;

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

}

package ar.edu.um.turnos.disponibilidad.infrastructure.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.disponibilidad.application.exception.RangoFechasInvalidoException;

@DisplayName("DisponibilidadExceptionHandler")
class DisponibilidadExceptionHandlerTest {

    private final DisponibilidadExceptionHandler handler = new DisponibilidadExceptionHandler();

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
    @DisplayName("RangoFechasInvalidoException se traduce a un 400 con code INVALID_DATE_RANGE")
    void rangoFechasInvalidoSeTraduceA400() {
        LocalDate from = LocalDate.of(2026, 9, 15);
        LocalDate to = LocalDate.of(2026, 9, 14);
        ProblemDetail problema = handler.manejarRangoFechasInvalido(new RangoFechasInvalidoException(from, to));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getProperties()).containsEntry("code", "INVALID_DATE_RANGE");
    }

}

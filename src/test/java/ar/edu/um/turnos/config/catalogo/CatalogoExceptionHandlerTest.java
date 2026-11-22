package ar.edu.um.turnos.config.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import ar.edu.um.turnos.config.catalogo.exception.CatalogoNoDisponibleException;

@DisplayName("CatalogoExceptionHandler")
class CatalogoExceptionHandlerTest {

    private final CatalogoExceptionHandler handler = new CatalogoExceptionHandler();

    @Test
    @DisplayName("CatalogoNoDisponibleException se traduce a un 503 con code CATALOGO_NO_DISPONIBLE (#17)")
    void catalogoNoDisponibleSeTraduceA503() {
        CatalogoNoDisponibleException excepcion = new CatalogoNoDisponibleException(
                "El catalogo no respondio tras agotar los reintentos", new RuntimeException("conexion rechazada"));

        ProblemDetail problema = handler.manejarCatalogoNoDisponible(excepcion);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problema.getProperties()).containsEntry("code", "CATALOGO_NO_DISPONIBLE");
    }

}

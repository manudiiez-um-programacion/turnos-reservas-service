package ar.edu.um.turnos.config.catalogo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.turnos.config.catalogo.exception.CatalogoNoDisponibleException;

/**
 * Traduce la indisponibilidad del catalogo (#17) a
 * {@code application/problem+json}: 503 con un {@code code} propio y
 * estable, para que cualquier endpoint que dependa del catalogo cuando
 * exista (M9 disponibilidad, M10 hold) falle rapido con un error
 * distinguible de uno funcional, sin que cada slice repita la traduccion.
 */
@RestControllerAdvice
public class CatalogoExceptionHandler {

    @ExceptionHandler(CatalogoNoDisponibleException.class)
    public ProblemDetail manejarCatalogoNoDisponible(CatalogoNoDisponibleException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "El catalogo no esta disponible, intente nuevamente mas tarde");
        problema.setTitle("Catalogo no disponible");
        problema.setProperty("code", "CATALOGO_NO_DISPONIBLE");
        return problema;
    }

}

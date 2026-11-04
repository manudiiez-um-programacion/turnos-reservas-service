package ar.edu.um.turnos.config.security;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;

/**
 * Escribe respuestas {@code application/problem+json} desde los puntos de
 * entrada de seguridad ({@link JwtAuthenticationEntryPoint},
 * {@link JwtAccessDeniedHandler}), que corren antes de que exista un
 * {@code @RestControllerAdvice} para manejarlas.
 *
 * <p>El mensaje de {@code detail} es fijo y generico a proposito: nunca el de
 * la excepcion de Spring Security, que puede describir por que fallo la
 * validacion del JWT (firma, formato, claim) y es informacion interna que no
 * se le debe filtrar a quien hizo el request.
 */
final class ProblemDetailResponses {

    // Instancia propia y no inyectada: estos handlers corren en el borde de la
    // cadena de filtros de seguridad, antes de que el contenedor de Spring MVC
    // (y su ObjectMapper autoconfigurado) entre en juego. Serializar un
    // ProblemDetail no necesita ninguna personalizacion del Jackson de la app.
    // El mixin es el que hace que setProperty("code", ...) aparezca como
    // "code" al tope del JSON (lo que el AdviceHandler logra via los
    // converters de Spring MVC): sin el, Jackson serializa las propiedades
    // extra anidadas bajo "properties".
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);

    private ProblemDetailResponses() {
    }

    static void escribir(HttpServletResponse response, HttpStatus status,
            String titulo, String detalle, String code) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setProperty("code", code);

        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        OBJECT_MAPPER.writeValue(response.getWriter(), problema);
    }

}

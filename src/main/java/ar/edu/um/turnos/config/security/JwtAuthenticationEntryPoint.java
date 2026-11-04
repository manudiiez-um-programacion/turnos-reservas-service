package ar.edu.um.turnos.config.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Responde {@code 401} en {@code application/problem+json} cuando el request
 * no trae un JWT valido (falta, vencio, firma invalida). Es lo que exige el
 * AC de #11: "un request sin token devuelve 401".
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ProblemDetailResponses.escribir(response, HttpStatus.UNAUTHORIZED,
                "No autenticado", "Se necesita un token valido para acceder a este recurso.",
                "NO_AUTENTICADO");
    }

}

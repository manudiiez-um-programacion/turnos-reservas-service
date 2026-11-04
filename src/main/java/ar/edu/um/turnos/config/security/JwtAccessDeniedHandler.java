package ar.edu.um.turnos.config.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde {@code 403} en {@code application/problem+json} cuando el JWT es
 * valido pero la identidad autenticada no tiene el permiso que el recurso
 * exige. Es lo que exige el AC de #11: "con token valido pero sin permisos,
 * 403".
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetailResponses.escribir(response, HttpStatus.FORBIDDEN,
                "Acceso denegado", "No tenes permisos para acceder a este recurso.",
                "ACCESO_DENEGADO");
    }

}

package ar.edu.um.turnos.config;

import java.util.List;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origenes permitidos para CORS (#11).
 *
 * <p>El enunciado (§9) pide configurar CORS "de acuerdo con los clientes
 * permitidos": una lista explicita, no el comodin {@code *}. Sin valor
 * configurado la lista queda vacia, que es el default mas seguro (ningun
 * origen cruzado permitido) y no impide arrancar: a diferencia de
 * {@link CatedraProperties} y {@link JwtProperties}, esto no es informacion
 * que la catedra entregue, sino una decision de despliegue.
 *
 * @param allowedOrigins origenes exactos permitidos (p. ej.
 *                        {@code https://app.ejemplo.com}), separados por
 *                        coma en {@code CORS_ALLOWED_ORIGINS}
 */
@Validated
@ConfigurationProperties(prefix = "turnos.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    /** Rechaza el arranque si alguien puso el comodin en vez de listar los origenes. */
    @AssertTrue(message = "turnos.cors.allowed-origins no puede contener '*': "
            + "hay que listar los origenes permitidos explicitamente")
    public boolean isSinComodin() {
        return allowedOrigins.stream().noneMatch("*"::equals);
    }

}

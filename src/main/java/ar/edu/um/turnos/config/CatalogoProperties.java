package ar.edu.um.turnos.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Acceso REST a la agenda vigente del servicio de catalogo (#16).
 *
 * <p>A diferencia de {@link CatedraProperties#api()}, la URL base <strong>si</strong>
 * tiene default: no es un dato que entregue un tercero por otro canal, es la
 * direccion del otro servicio propio del mismo proyecto, que en desarrollo
 * corre en la misma maquina en el puerto 8080 (§5/§13 de CLAUDE.md, mientras
 * no exista el compose de nivel superior que una los dos). El JWT con el que
 * se autentica cada llamada no esta aca: lo emite {@code InternalJwtIssuer}
 * con el secreto de {@link JwtProperties#internalSecret()} (#12), sin cache,
 * en cada request.
 *
 * <p>Timeouts y reintentos son ajuste operativo, con los mismos defaults que
 * {@link CatedraProperties.Api}, y solo se aplican a la unica operacion que
 * este cliente expone por ahora: la agenda vigente, un GET idempotente.
 *
 * @param baseUrl        URL base del servicio de catalogo, sin barra final
 * @param connectTimeout timeout de conexion del cliente REST
 * @param readTimeout    timeout de lectura del cliente REST
 * @param maxRetries     cantidad maxima de reintentos ante un 503 o error de red
 * @param retryBackoff   espera base entre reintentos (crece exponencialmente)
 */
@Validated
@ConfigurationProperties(prefix = "catalogo.api")
public record CatalogoProperties(

        @NotBlank(message = "falta CATALOGO_API_BASE_URL")
        String baseUrl,

        @NotNull(message = "falta CATALOGO_API_CONNECT_TIMEOUT")
        Duration connectTimeout,

        @NotNull(message = "falta CATALOGO_API_READ_TIMEOUT")
        Duration readTimeout,

        @Min(value = 0, message = "CATALOGO_API_MAX_RETRIES no puede ser negativo")
        int maxRetries,

        @NotNull(message = "falta CATALOGO_API_RETRY_BACKOFF")
        Duration retryBackoff

) {
}

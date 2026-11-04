package ar.edu.um.turnos.config.catedra;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.CatedraProperties;

/**
 * Agrega a cada llamada saliente al servicio central de la catedra el JWT
 * tecnico de la cuenta del proyecto y el header de correlacion (#14).
 *
 * <p>A diferencia del interno hacia el catalogo ({@code CatalogoAuthInterceptor}),
 * este JWT no se emite: es estatico, lo entrega la catedra
 * ({@link CatedraProperties#jwt()}) y se reenvia tal cual en cada request.
 * <strong>Nunca se loguea</strong>: este interceptor no escribe ningun log, y
 * el JWT no vuelve a aparecer en ningun punto mas de la cadena (ni en
 * excepciones, ni en el cliente que lo usa).
 *
 * <p>El header {@code X-Correlation-Id} sigue la misma convencion que el del
 * catalogo: si el llamador ya puso uno, se respeta; si no, se genera uno
 * nuevo.
 */
@Component
@RequiredArgsConstructor
public class CatedraAuthInterceptor implements ClientHttpRequestInterceptor {

    static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final CatedraProperties catedraProperties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().setBearerAuth(catedraProperties.jwt());
        if (!request.getHeaders().containsHeader(HEADER_CORRELATION_ID)) {
            request.getHeaders().add(HEADER_CORRELATION_ID, UUID.randomUUID().toString());
        }
        return execution.execute(request, body);
    }

}

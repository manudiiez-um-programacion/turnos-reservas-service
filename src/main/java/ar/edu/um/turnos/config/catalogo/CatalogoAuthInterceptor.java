package ar.edu.um.turnos.config.catalogo;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Agrega a cada llamada saliente al catalogo el JWT tecnico interno y el
 * header de correlacion (#12). Lo registra {@link CatalogoRestClientConfig}
 * en el {@code RestClient} que llama a la agenda vigente (#16): no define
 * uno propio porque la URL base y los timeouts son responsabilidad de esa
 * configuracion, no de este interceptor.
 *
 * <p>El header {@code X-Correlation-Id} es, segun el ADR 0002, lo que
 * reemplaza a la identidad del usuario para la trazabilidad (el token tecnico
 * no representa a ningun usuario). Si el llamador ya puso uno —por ejemplo
 * para encadenarlo con el de un request entrante del usuario— se respeta y
 * no se pisa; si no, se genera uno nuevo.
 */
@Component
@RequiredArgsConstructor
public class CatalogoAuthInterceptor implements ClientHttpRequestInterceptor {

    static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final InternalJwtIssuer internalJwtIssuer;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().setBearerAuth(internalJwtIssuer.emitir());
        if (!request.getHeaders().containsHeader(HEADER_CORRELATION_ID)) {
            request.getHeaders().add(HEADER_CORRELATION_ID, UUID.randomUUID().toString());
        }
        return execution.execute(request, body);
    }

}

package ar.edu.um.turnos.config.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import ar.edu.um.turnos.config.JwtProperties;

/**
 * Verifica que el interceptor agrega el Bearer y el header de correlacion
 * (#12), y que respeta un {@code X-Correlation-Id} que el llamador ya haya
 * puesto en lugar de pisarlo.
 */
class CatalogoAuthInterceptorTest {

    private final JwtProperties jwtProperties = new JwtProperties(
            "turnos-reservas-service", "secreto-de-usuario-de-prueba-0000000000000000000",
            "secreto-interno-de-prueba-de-al-menos-32-char", Duration.ofHours(1), Duration.ofMinutes(2));

    private final CatalogoAuthInterceptor interceptor = new CatalogoAuthInterceptor(new InternalJwtIssuer(jwtProperties));

    @Test
    void agregaElHeaderAuthorizationConBearer() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], 200));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");
    }

    @Test
    void agregaUnCorrelationIdSiNoHayUnoYa() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], 200));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(CatalogoAuthInterceptor.HEADER_CORRELATION_ID)).isNotBlank();
    }

    @Test
    void respetaElCorrelationIdQueYaTraeElRequest() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.getHeaders().add(CatalogoAuthInterceptor.HEADER_CORRELATION_ID, "correlation-del-llamador");
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], 200));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(CatalogoAuthInterceptor.HEADER_CORRELATION_ID))
                .isEqualTo("correlation-del-llamador");
    }

}

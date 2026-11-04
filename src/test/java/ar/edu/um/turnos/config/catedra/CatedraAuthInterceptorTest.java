package ar.edu.um.turnos.config.catedra;

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

import ar.edu.um.turnos.config.CatedraProperties;

/**
 * Verifica que el interceptor agrega el Bearer con el JWT tecnico estatico de
 * la catedra y el header de correlacion (#14), con el mismo comportamiento
 * que su analogo hacia el catalogo.
 */
class CatedraAuthInterceptorTest {

    private final CatedraProperties catedraProperties = new CatedraProperties(
            "grupo-de-prueba",
            "jwt-tecnico-de-prueba-no-es-una-credencial",
            new CatedraProperties.Api(
                    "http://catedra.invalido:9999", Duration.ofSeconds(2), Duration.ofSeconds(5), 3, Duration.ofMillis(200)),
            new CatedraProperties.Kafka("kafka.invalido:9092", "grupo", "topic.entrada", "topic.salida"),
            null);

    private final CatedraAuthInterceptor interceptor = new CatedraAuthInterceptor(catedraProperties);

    @Test
    void agregaElHeaderAuthorizationConElJwtDeLaCatedra() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], 200));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer jwt-tecnico-de-prueba-no-es-una-credencial");
    }

    @Test
    void agregaUnCorrelationIdSiNoHayUnoYa() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], 200));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(CatedraAuthInterceptor.HEADER_CORRELATION_ID)).isNotBlank();
    }

    @Test
    void respetaElCorrelationIdQueYaTraeElRequest() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.getHeaders().add(CatedraAuthInterceptor.HEADER_CORRELATION_ID, "correlation-del-llamador");
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], 200));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(CatedraAuthInterceptor.HEADER_CORRELATION_ID))
                .isEqualTo("correlation-del-llamador");
    }

}

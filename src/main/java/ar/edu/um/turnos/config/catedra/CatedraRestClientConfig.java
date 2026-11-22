package ar.edu.um.turnos.config.catedra;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import ar.edu.um.turnos.config.CatedraProperties;

/**
 * Arma el {@link RestClient} hacia el servicio central de la catedra (#14):
 * URL base y timeouts desde {@link CatedraProperties#api()}, con el JWT
 * tecnico agregado por {@link CatedraAuthInterceptor} en cada llamada.
 *
 * <p>Los timeouts son explicitos a proposito: sin ellos, un socket colgado
 * dejaria esperando indefinidamente al hilo que atiende el request del
 * usuario (§18.4 de la referencia — un timeout no prueba que la operacion
 * haya fallado, pero tampoco hay que esperarla para siempre).
 */
@Configuration
@RequiredArgsConstructor
public class CatedraRestClientConfig {

    private final CatedraProperties catedraProperties;
    private final CatedraAuthInterceptor catedraAuthInterceptor;

    @Bean
    RestClient catedraRestClient() {
        CatedraProperties.Api api = catedraProperties.api();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(api.connectTimeout());
        requestFactory.setReadTimeout(api.readTimeout());

        return RestClient.builder()
                .baseUrl(api.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(catedraAuthInterceptor)
                .build();
    }

}

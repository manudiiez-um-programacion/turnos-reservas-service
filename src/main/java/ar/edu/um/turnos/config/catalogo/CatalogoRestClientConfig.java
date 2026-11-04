package ar.edu.um.turnos.config.catalogo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import ar.edu.um.turnos.config.CatalogoProperties;

/**
 * Arma el {@link RestClient} hacia la agenda vigente del servicio de
 * catalogo (#16): URL base y timeouts desde {@link CatalogoProperties}, con
 * el JWT tecnico interno agregado por {@link CatalogoAuthInterceptor} en
 * cada llamada.
 *
 * <p>Timeouts explicitos por la misma razon que en
 * {@code CatedraRestClientConfig}: sin ellos, un socket colgado dejaria
 * esperando indefinidamente al hilo que atiende el request del usuario.
 */
@Configuration
@RequiredArgsConstructor
public class CatalogoRestClientConfig {

    private final CatalogoProperties catalogoProperties;
    private final CatalogoAuthInterceptor catalogoAuthInterceptor;

    @Bean
    RestClient catalogoRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(catalogoProperties.connectTimeout());
        requestFactory.setReadTimeout(catalogoProperties.readTimeout());

        return RestClient.builder()
                .baseUrl(catalogoProperties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(catalogoAuthInterceptor)
                .build();
    }

}

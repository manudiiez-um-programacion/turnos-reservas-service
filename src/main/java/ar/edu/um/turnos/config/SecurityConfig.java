package ar.edu.um.turnos.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ar.edu.um.turnos.config.security.JwtAccessDeniedHandler;
import ar.edu.um.turnos.config.security.JwtAuthenticationEntryPoint;

/**
 * Configuracion de seguridad definitiva (#11): solo {@code /api/register},
 * {@code /api/authenticate}, el estado del servicio y la documentacion de la
 * API quedan publicos; todo lo demas exige un JWT de usuario valido.
 *
 * <p>{@code /api/health} y Swagger UI/OpenAPI se tratan como infraestructura
 * y no como funcionalidad de negocio: el healthcheck de {@code compose.yaml}
 * los consulta sin credenciales (no tiene forma de llevar un token), y
 * Swagger UI es la herramienta de exploracion de la API en desarrollo. El AC
 * de #11 ("solo registro y login son publicos") apunta a los endpoints de
 * negocio, que hoy son unicamente esos dos.
 *
 * <p>Queda sin sesiones ({@code STATELESS}) y sin CSRF, como ya fijaba la
 * version provisional: la API se autentica por token, no por cookie, asi que
 * no hay vector CSRF que mitigar y el filtro solo le complicaria la vida a un
 * cliente no-navegador (la app KMP).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String CLAIM_AUTORIDADES = "auth";

    /** Infraestructura y endpoints explicitamente publicos. Ver el Javadoc de la clase. */
    private static final String[] RUTAS_PUBLICAS = {
            "/api/register",
            "/api/authenticate",
            "/api/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    // java:S4502 avisa que deshabilitar CSRF puede ser inseguro. Aca no lo es: la
    // proteccion CSRF existe porque el navegador adjunta la cookie de sesion sola;
    // esta API no usa cookies ni sesiones (STATELESS) y se autentica por un token
    // que el cliente tiene que poner a mano en cada request, asi que un sitio de
    // terceros no puede fabricar una peticion autenticada.
    @SuppressWarnings("java:S4502")
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El preflight de CORS nunca lleva el Authorization header: sin este
                        // permitAll, un navegador real jamas llegaria a mandar el request
                        // autenticado porque el preflight caeria con 401 antes.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling.accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /**
     * Decodifica y valida el JWT de usuario que emite {@code JwtIssuerAdapter}:
     * misma clave HS256 ({@code turnos.jwt.userSecret}), firma, vigencia
     * ({@code exp}/{@code nbf}, via {@link JwtValidators}) e {@code iss}.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec clave = new SecretKeySpec(
                jwtProperties.userSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
        return decoder;
    }

    /**
     * Lee las autoridades del claim {@code auth} (arreglo, no string separado
     * por espacios) y las usa tal cual: ya vienen con el prefijo {@code ROLE_}
     * puesto por {@code RegistrarUsuarioUseCaseImpl}, asi que no hay que
     * agregar otro. El principal sigue siendo el claim {@code sub} (el
     * {@code login}): nunca un id que mande el cliente.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(CLAIM_AUTORIDADES);
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * Lista explicita de origenes permitidos ({@link CorsProperties}), nunca
     * {@code *}. Sin origenes configurados no se permite ningun cross-origin,
     * que es el default seguro.
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

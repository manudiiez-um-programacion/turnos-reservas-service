package ar.edu.um.turnos.config.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import ar.edu.um.turnos.config.JwtProperties;

/**
 * Verifica, de punta a punta, que el token que emite {@link InternalJwtIssuer}
 * y el header que agrega {@link CatalogoAuthInterceptor} son lo que el
 * catalogo real espera (#12): firma HS256 con el secreto interno, claim
 * {@code authorities} con el valor exacto {@code SERVICE_TURNOS} y sin
 * prefijo, e {@code iss} coincidente.
 *
 * <p>No hay una instancia real del catalogo en este repo ni en este CI: el
 * servidor de prueba replica exactamente la logica de su cadena
 * {@code apiInterna} (decodificador HS256 + {@code hasAuthority
 * SERVICE_TURNOS}), tal como esta implementada en
 * {@code turnos-catalogo-sync}, issue #17 (ya cerrado del otro lado). Es un
 * test de contrato: si el catalogo cambiara esa validacion, este test seguiria
 * pasando con la version vieja y fallaria con un catalogo de verdad, que es el
 * motivo por el que el contrato esta citado en el Javadoc y no solo en un
 * comentario.
 */
@DisplayName("Interceptor del JWT interno contra un catalogo simulado")
class CatalogoRestClientIntegrationTest {

    private static final String SECRETO_INTERNO = "secreto-interno-de-prueba-de-al-menos-32-char";
    private static final String OTRO_SECRETO = "otro-secreto-que-el-catalogo-no-conoce-000000000";
    private static final String ISSUER = "turnos-reservas-service-test";

    private final JwtProperties jwtProperties = new JwtProperties(
            ISSUER, "secreto-de-usuario-no-se-usa-aca-000000000000000", SECRETO_INTERNO,
            Duration.ofHours(1), Duration.ofMinutes(2));

    private final CatalogoAuthInterceptor interceptor =
            new CatalogoAuthInterceptor(new InternalJwtIssuer(jwtProperties));

    private HttpServer servidorFalso;
    private RestClient restClient;

    @BeforeEach
    void levantarServidorFalso() throws Exception {
        servidorFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidorFalso.createContext("/api/internal/ocupaciones", exchange -> {
            int status = validar(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        servidorFalso.start();

        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + servidorFalso.getAddress().getPort())
                .requestInterceptor(interceptor)
                .build();
    }

    @AfterEach
    void apagarServidorFalso() {
        servidorFalso.stop(0);
    }

    /** Replica la cadena "apiInterna" del catalogo: HS256 + exp/iss + hasAuthority("SERVICE_TURNOS"). */
    private int validar(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return HttpStatus.UNAUTHORIZED.value();
        }
        String token = authorizationHeader.substring("Bearer ".length());
        try {
            SecretKey clave = Keys.hmacShaKeyFor(SECRETO_INTERNO.getBytes(StandardCharsets.UTF_8));
            Jws<Claims> jws = Jwts.parser().verifyWith(clave).requireIssuer(ISSUER).build().parseSignedClaims(token);

            @SuppressWarnings("unchecked")
            List<String> autoridades = jws.getPayload().get("authorities", List.class);
            if (autoridades == null || !autoridades.contains("SERVICE_TURNOS")) {
                return HttpStatus.FORBIDDEN.value();
            }
            return HttpStatus.OK.value();
        } catch (io.jsonwebtoken.JwtException e) {
            return HttpStatus.UNAUTHORIZED.value();
        }
    }

    @Test
    @DisplayName("el interceptor emite un token que el catalogo acepta")
    void elTokenDelInterceptorEsAceptado() {
        var respuesta = restClient.get().uri("/api/internal/ocupaciones").retrieve().toBodilessEntity();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("sin Authorization, el catalogo rechaza con 401")
    void sinTokenElCatalogoRechaza() {
        RestClient sinInterceptor = RestClient.create("http://localhost:" + servidorFalso.getAddress().getPort());

        assertThatThrownBy(() -> sinInterceptor.get().uri("/api/internal/ocupaciones").retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @DisplayName("con un token firmado con otro secreto, el catalogo rechaza con 401")
    void tokenConOtroSecretoElCatalogoRechaza() {
        SecretKey otraClave = Keys.hmacShaKeyFor(OTRO_SECRETO.getBytes(StandardCharsets.UTF_8));
        String tokenFalsificado = Jwts.builder()
                .issuer(ISSUER)
                .claim("authorities", List.of("SERVICE_TURNOS"))
                .signWith(otraClave, Jwts.SIG.HS256)
                .compact();

        RestClient sinInterceptor = RestClient.create("http://localhost:" + servidorFalso.getAddress().getPort());

        assertThatThrownBy(() -> sinInterceptor.get()
                        .uri("/api/internal/ocupaciones")
                        .header("Authorization", "Bearer " + tokenFalsificado)
                        .retrieve()
                        .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    @DisplayName("un token sin la autoridad SERVICE_TURNOS es rechazado con 403")
    void tokenSinLaAutoridadElCatalogoRechaza() {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_INTERNO.getBytes(StandardCharsets.UTF_8));
        String tokenSinAutoridad = Jwts.builder()
                .issuer(ISSUER)
                .claim("authorities", List.of("OTRA_COSA"))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();

        RestClient sinInterceptor = RestClient.create("http://localhost:" + servidorFalso.getAddress().getPort());

        assertThatThrownBy(() -> sinInterceptor.get()
                        .uri("/api/internal/ocupaciones")
                        .header("Authorization", "Bearer " + tokenSinAutoridad)
                        .retrieve()
                        .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Forbidden.class);
    }

}

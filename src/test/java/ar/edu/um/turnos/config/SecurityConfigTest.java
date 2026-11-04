package ar.edu.um.turnos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Recorre la cadena de seguridad real (#11): solo registro, login y la
 * infraestructura (health, Swagger) quedan publicos; el resto exige un JWT
 * de usuario valido, y CORS no permite ningun origen sin configurar uno.
 *
 * <p>Los secretos y el issuer son los de {@code application-test.properties}
 * (perfil {@code test}), no credenciales reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    private static final String SECRETO_USUARIO = "secreto-de-usuario-solo-para-pruebas-0001";
    private static final String ISSUER = "turnos-reservas-service-test";

    /** Ruta de negocio inexistente: sirve para probar autenticacion sin depender de un endpoint real. */
    private static final String RUTA_PROTEGIDA = "/api/una-ruta-que-no-existe";

    @Autowired
    private MockMvc mockMvc;

    private static String token(String secreto, String issuer, Instant expiracion) {
        SecretKey clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("ana.gomez")
                .claim("auth", java.util.List.of("ROLE_USER"))
                .issuer(issuer)
                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                .expiration(Date.from(expiracion))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    private static String tokenValido() {
        return token(SECRETO_USUARIO, ISSUER, Instant.now().plusSeconds(3600));
    }

    @Nested
    @DisplayName("endpoints publicos")
    class EndpointsPublicos {

        @Test
        @DisplayName("el registro no exige token")
        void elRegistroNoExigeToken() throws Exception {
            // Cuerpo invalido a proposito: solo interesa que NO sea 401/403.
            mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("el login no exige token")
        void elLoginNoExigeToken() throws Exception {
            mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("el estado del servicio no exige token")
        void elHealthNoExigeToken() throws Exception {
            mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("la documentacion OpenAPI no exige token")
        void laDocumentacionNoExigeToken() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("autenticacion")
    class Autenticacion {

        @Test
        @DisplayName("sin token, un endpoint protegido devuelve 401 en problem+json")
        void sinTokenDevuelve401() throws Exception {
            mockMvc.perform(get(RUTA_PROTEGIDA))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"))
                    .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        }

        @Test
        @DisplayName("con un token valido, pasa la autenticacion (404: la ruta no existe)")
        void conTokenValidoPasaLaAutenticacion() throws Exception {
            mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("un token firmado con otro secreto devuelve 401")
        void tokenConOtroSecretoDevuelve401() throws Exception {
            String token = token("otro-secreto-que-no-es-el-correcto-0000000000000", ISSUER,
                    Instant.now().plusSeconds(3600));

            mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un token vencido devuelve 401")
        void tokenVencidoDevuelve401() throws Exception {
            // Mas de 60s en el pasado: JwtTimestampValidator tolera un clock skew
            // default de 60s, asi que un vencimiento mas reciente todavia pasaria.
            String token = token(SECRETO_USUARIO, ISSUER, Instant.now().minusSeconds(120));

            mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un token con otro issuer devuelve 401")
        void tokenConOtroIssuerDevuelve401() throws Exception {
            String token = token(SECRETO_USUARIO, "otro-emisor", Instant.now().plusSeconds(3600));

            mockMvc.perform(get(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CORS")
    class Cors {

        @Test
        @DisplayName("sin origenes configurados, un pedido cruzado se rechaza")
        void sinOrigenesConfiguradosSeRechazaElPedidoCruzado() throws Exception {
            // El CorsFilter de Spring rechaza con 403 antes de llegar a la cadena de
            // autorizacion: ni un origen permitido, ni token, hacen pasar un pedido
            // cuyo Origin no esta en la lista explicita (#11, AC: "no *").
            mockMvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, "https://cualquiera.test"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("sin header Origin, no es un pedido CORS y sigue publico")
        void sinHeaderOrigenNoEsUnPedidoCors() throws Exception {
            mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        }
    }

}

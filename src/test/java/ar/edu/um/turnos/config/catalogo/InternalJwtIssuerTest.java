package ar.edu.um.turnos.config.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.config.JwtProperties;

/**
 * Verifica que el JWT tecnico interno respeta el contrato que ya valida el
 * catalogo (#12, ADR 0002): claim {@code authorities} con
 * {@code SERVICE_TURNOS} sin prefijo, firmado con el secreto interno y no con
 * el de usuario.
 */
class InternalJwtIssuerTest {

    private static final String SECRETO_INTERNO = "secreto-interno-de-prueba-de-al-menos-32-char";
    private static final String SECRETO_USUARIO = "secreto-de-usuario-de-prueba-distinto-0000000000";

    private final JwtProperties jwtProperties = new JwtProperties(
            "turnos-reservas-service", SECRETO_USUARIO, SECRETO_INTERNO,
            Duration.ofHours(1), Duration.ofMinutes(2));

    private final InternalJwtIssuer issuer = new InternalJwtIssuer(jwtProperties);

    private Claims parsear(String token) {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_INTERNO.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token);
        return jws.getPayload();
    }

    @Test
    void elClaimAuthoritiesTraeExactamenteServiceTurnos() {
        String token = issuer.emitir();

        @SuppressWarnings("unchecked")
        List<String> autoridades = parsear(token).get("authorities", List.class);
        assertThat(autoridades).containsExactly("SERVICE_TURNOS");
    }

    @Test
    void elIssuerEsElConfigurado() {
        String token = issuer.emitir();

        assertThat(parsear(token).getIssuer()).isEqualTo("turnos-reservas-service");
    }

    @Test
    void laVigenciaCoincideConInternalExpiration() {
        String token = issuer.emitir();
        Claims claims = parsear(token);

        long vigenciaMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(vigenciaMs).isEqualTo(jwtProperties.internalExpiration().toMillis());
    }

    @Test
    void noVerificaConElSecretoDeUsuario() {
        String token = issuer.emitir();
        SecretKey claveDeUsuario = Keys.hmacShaKeyFor(SECRETO_USUARIO.getBytes(StandardCharsets.UTF_8));

        org.junit.jupiter.api.Assertions.assertThrows(
                io.jsonwebtoken.security.SignatureException.class,
                () -> Jwts.parser().verifyWith(claveDeUsuario).build().parseSignedClaims(token));
    }

}

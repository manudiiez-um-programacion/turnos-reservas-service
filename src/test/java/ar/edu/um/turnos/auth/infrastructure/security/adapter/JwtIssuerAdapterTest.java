package ar.edu.um.turnos.auth.infrastructure.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.config.JwtProperties;

/**
 * Verifica que el token emitido respeta el contrato con el servicio de
 * catalogo (ADR 0002, turnos-catalogo-sync#59): claims, nombres y firma.
 */
class JwtIssuerAdapterTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-32-caracteres-0001";

    private final JwtProperties jwtProperties = new JwtProperties(
            "turnos-reservas-service", SECRETO, "otro-secreto-de-prueba-distinto-0002000000000000",
            Duration.ofHours(1), Duration.ofMinutes(2));

    private final JwtIssuerAdapter adapter = new JwtIssuerAdapter(jwtProperties);

    private Jws<Claims> parsear(String token) {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token);
    }

    @Test
    void elSubjectEsElLoginDelUsuario() {
        Usuario usuario = Usuario.builder().login("ana.gomez").authorities(Set.of("ROLE_USER")).build();

        String token = adapter.emitir(usuario);

        assertThat(parsear(token).getPayload().getSubject()).isEqualTo("ana.gomez");
    }

    @Test
    void elClaimAuthTraeLasAutoridadesComoArreglo() {
        Usuario usuario = Usuario.builder().login("ana.gomez").authorities(Set.of("ROLE_USER")).build();

        String token = adapter.emitir(usuario);

        @SuppressWarnings("unchecked")
        List<String> autoridades = parsear(token).getPayload().get("auth", List.class);
        assertThat(autoridades).containsExactly("ROLE_USER");
    }

    @Test
    void elIssuerEsElConfiguradoEnJwtProperties() {
        Usuario usuario = Usuario.builder().login("ana.gomez").authorities(Set.of("ROLE_USER")).build();

        String token = adapter.emitir(usuario);

        assertThat(parsear(token).getPayload().getIssuer()).isEqualTo("turnos-reservas-service");
    }

    @Test
    void laVigenciaCoincideConUserExpiration() {
        Usuario usuario = Usuario.builder().login("ana.gomez").authorities(Set.of("ROLE_USER")).build();

        String token = adapter.emitir(usuario);
        Claims claims = parsear(token).getPayload();

        long vigenciaMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(vigenciaMs).isEqualTo(jwtProperties.userExpiration().toMillis());
    }

    @Test
    void unTokenFirmadoConOtroSecretoNoVerifica() {
        Usuario usuario = Usuario.builder().login("ana.gomez").authorities(Set.of("ROLE_USER")).build();
        String token = adapter.emitir(usuario);

        SecretKey otraClave = Keys.hmacShaKeyFor("otro-secreto-que-no-es-el-correcto-00000000000".getBytes(StandardCharsets.UTF_8));

        org.junit.jupiter.api.Assertions.assertThrows(
                io.jsonwebtoken.security.SignatureException.class,
                () -> Jwts.parser().verifyWith(otraClave).build().parseSignedClaims(token));
    }

}

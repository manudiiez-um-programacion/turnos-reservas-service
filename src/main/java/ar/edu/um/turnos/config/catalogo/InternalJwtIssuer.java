package ar.edu.um.turnos.config.catalogo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.JwtProperties;

/**
 * Emite el JWT tecnico interno con el que este servicio llama a la agenda
 * vigente del catalogo (#12, D4 del ADR 0002 de {@code turnos-catalogo-sync#59}).
 *
 * <p>Formato y claims son <strong>contrato</strong> con el validador que ya
 * implementa el catalogo ({@code SecurityConfig} de ese repo, cadena
 * {@code apiInterna}, cerrada en su issue #17): firma HS256 con
 * {@code turnos.jwt.internalSecret} ({@code JWT_INTERNAL_SECRET}), claim de
 * autoridades llamado <strong>{@code authorities}</strong> (no {@code auth},
 * que es el del usuario final) con el valor exacto {@code SERVICE_TURNOS} y
 * <strong>sin</strong> el prefijo {@code ROLE_} que usan los roles de
 * usuario: no es un rol, es la identidad de un servicio. El {@code iss} tiene
 * que coincidir con el {@code JWT_INTERNAL_ISSUER} que el catalogo valida del
 * otro lado; por default los dos valen {@code turnos-reservas-service}.
 *
 * <p>Vigencia corta ({@code turnos.jwt.internalExpiration}, default
 * {@code PT2M}) y <strong>sin cache</strong>: se emite un token nuevo en cada
 * llamada en lugar de guardar uno para reusar. Para una vigencia de minutos,
 * cachear agregaria la complejidad de invalidar antes del vencimiento sin
 * ahorrar casi nada; emitir de nuevo es mas simple y nunca presenta un token
 * vencido por error. El token no se loguea en ningun punto.
 */
@Component
@RequiredArgsConstructor
public class InternalJwtIssuer {

    /** Claim de autoridades del token tecnico interno. Distinto del "auth" del usuario final. */
    private static final String CLAIM_AUTORIDADES = "authorities";

    /** Unica autoridad que el catalogo exige en su API interna. Sin prefijo ROLE_. */
    private static final String AUTORIDAD_SERVICE_TURNOS = "SERVICE_TURNOS";

    private final JwtProperties jwtProperties;

    public String emitir() {
        SecretKey clave = Keys.hmacShaKeyFor(jwtProperties.internalSecret().getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();

        return Jwts.builder()
                .subject("turnos-reservas-service")
                .claim(CLAIM_AUTORIDADES, List.of(AUTORIDAD_SERVICE_TURNOS))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(jwtProperties.internalExpiration())))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

}

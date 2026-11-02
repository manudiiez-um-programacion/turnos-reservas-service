package ar.edu.um.turnos.auth.infrastructure.security.adapter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.out.JwtIssuerPort;
import ar.edu.um.turnos.config.JwtProperties;
import lombok.RequiredArgsConstructor;

/**
 * Emite el JWT de usuario. Formato y claims son <strong>contrato con el
 * servicio de catalogo</strong> (ADR 0002, {@code turnos-catalogo-sync#59}),
 * no un detalle interno de este servicio: cambiarlos rompe la validacion del
 * otro lado sin coordinar.
 *
 * <ul>
 *   <li>{@code sub}: el {@code login} del usuario. Es lo unico que el ADR
 *       exige ("un identificador estable, para los logs y nada mas") y
 *       coincide con lo que hace JHipster (el subject del token es el
 *       username), que es el modelo con el que §3.2 del enunciado pide ser
 *       compatible. No es el {@code externalPatientId}: ese identificador es
 *       para la cuenta tecnica de la catedra, no para que lo lea el otro
 *       backend propio.</li>
 *   <li>{@code auth}: las autoridades, como arreglo. El nombre del claim y
 *       que sea un arreglo (no una lista separada por comas) no son
 *       arbitrarios: {@code SecurityConfig} del repo de catalogo configura su
 *       {@code JwtGrantedAuthoritiesConverter} con
 *       {@code setAuthoritiesClaimName("auth")}, y ese converter solo separa
 *       por espacios cuando el claim es un string — un arreglo evita
 *       depender de ese detalle de separador.</li>
 *   <li>{@code iss}: {@code turnos.jwt.issuer}, que el catalogo valida contra
 *       su propio {@code JWT_USER_ISSUER} (el mismo valor por defecto en los
 *       dos repos: {@code turnos-reservas-service}).</li>
 *   <li>{@code iat} / {@code exp}: emision y vigencia
 *       ({@code turnos.jwt.userExpiration}).</li>
 * </ul>
 *
 * <p>Nunca lleva email, el {@code externalPatientId} ni nada de la cuenta
 * tecnica de la catedra (#10, AC: "no transporta datos sensibles").
 *
 * <p>Firma con <strong>jjwt</strong>, la misma libreria que usa el
 * {@code TokenProvider} que genera JHipster: no hace falta
 * {@code spring-security-oauth2-jose} (ese es para decodificar como resource
 * server, que es lo que hace falta en #11 para validar tokens entrantes, no
 * para emitir este).
 */
@Component
@RequiredArgsConstructor
public class JwtIssuerAdapter implements JwtIssuerPort {

    private static final String CLAIM_AUTORIDADES = "auth";

    private final JwtProperties jwtProperties;

    @Override
    public String emitir(Usuario usuario) {
        SecretKey clave = Keys.hmacShaKeyFor(jwtProperties.userSecret().getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();

        return Jwts.builder()
                .subject(usuario.getLogin())
                .claim(CLAIM_AUTORIDADES, usuario.getAuthorities())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(jwtProperties.userExpiration())))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

}

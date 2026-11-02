package ar.edu.um.turnos.auth.infrastructure.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hasheo de contrasenas con <strong>Argon2id</strong> (indicacion del
 * profesor, 28-10-2026: BCrypt quedo debilitado frente al poder de computo
 * actual).
 *
 * <p>Parametros <strong>explicitos</strong> a proposito, en vez de
 * {@code Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()}: el criterio
 * de aceptacion de #9 pide parametros documentados, no los defaults
 * implicitos de la libreria (que ademas podrian cambiar de version a
 * version sin que el codigo lo refleje).
 *
 * <p>Valores de partida de la <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">
 * recomendacion OWASP 2023</a> para Argon2id (memoria 19 MiB, 2 iteraciones,
 * paralelismo 1), <strong>medidos</strong> en esta maquina y no copiados a
 * ciegas (decision pendiente del §9 de CLAUDE.md): un {@code encode()}
 * tarda entre 20 y 25&nbsp;ms en estado estable, y el string codificado mide
 * 97 caracteres (105 con el prefijo {@code {argon2}} que agrega el
 * {@link DelegatingPasswordEncoder}) &mdash; de ahi que
 * {@code usuario.password_hash} sea {@code VARCHAR(120)} en
 * {@code V2__usuarios.sql} y no los 60 caracteres fijos de BCrypt.
 *
 * <p>El prefijo {@code {argon2}} que antepone el
 * {@code DelegatingPasswordEncoder} es lo que permite cambiar de algoritmo o
 * de parametros a futuro sin invalidar los hashes existentes: al verificar,
 * delega segun el prefijo guardado, no segun el encoder configurado hoy.
 */
@Configuration
public class PasswordEncoderConfig {

    private static final String ID_ARGON2 = "argon2";

    /** Bytes de sal por hash. */
    private static final int SALT_LENGTH = 16;

    /** Bytes del hash resultante (sin contar el header ni la sal). */
    private static final int HASH_LENGTH = 32;

    /** Hilos usados para calcular el hash. */
    private static final int PARALLELISM = 1;

    /** Memoria en KiB (19 MiB), el parametro que mas le cuesta forzar a un atacante. */
    private static final int MEMORIA_KIB = 19456;

    /** Pasadas sobre la memoria. */
    private static final int ITERACIONES = 2;

    @Bean
    public PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
                SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORIA_KIB, ITERACIONES);
        return new DelegatingPasswordEncoder(ID_ARGON2, Map.of(ID_ARGON2, argon2));
    }

}

package ar.edu.um.turnos.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * No levanta contexto de Spring a proposito: el encoder no depende de nada
 * del framework mas alla de la libreria de crypto, y una prueba unitaria
 * simple alcanza para documentar y verificar sus propiedades.
 */
class PasswordEncoderConfigTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void produceUnHashConElPrefijoArgon2() {
        String hash = passwordEncoder.encode("una-contrasenia-de-prueba");

        assertThat(hash).startsWith("{argon2}$argon2id$");
        // 120 es el tamano de usuario.password_hash en V2__usuarios.sql.
        assertThat(hash.length()).isLessThanOrEqualTo(120);
    }

    @Test
    void verificaUnaContraseniaCorrecta() {
        String hash = passwordEncoder.encode("una-contrasenia-de-prueba");

        assertThat(passwordEncoder.matches("una-contrasenia-de-prueba", hash)).isTrue();
        assertThat(passwordEncoder.matches("otra-distinta", hash)).isFalse();
    }

}

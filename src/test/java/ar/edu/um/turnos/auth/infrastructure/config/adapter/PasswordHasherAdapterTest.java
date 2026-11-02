package ar.edu.um.turnos.auth.infrastructure.config.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordHasherAdapterTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void delegaElHasheoEnElPasswordEncoderDeSpringSecurity() {
        when(passwordEncoder.encode("una-contrasenia")).thenReturn("{argon2}hash");

        PasswordHasherAdapter adapter = new PasswordHasherAdapter(passwordEncoder);

        assertThat(adapter.hashear("una-contrasenia")).isEqualTo("{argon2}hash");
    }

}

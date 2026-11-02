package ar.edu.um.turnos.auth.infrastructure.config.adapter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.ports.out.PasswordHasherPort;
import lombok.RequiredArgsConstructor;

/** Adapta el {@link PasswordEncoder} de Spring Security al puerto del dominio. */
@Component
@RequiredArgsConstructor
public class PasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hashear(String passwordEnTextoPlano) {
        return passwordEncoder.encode(passwordEnTextoPlano);
    }

}

package ar.edu.um.turnos.auth.application.usecases;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.application.exception.UsuarioDuplicadoException;
import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.RegistrarUsuarioUseCase;
import ar.edu.um.turnos.auth.domain.ports.out.PasswordHasherPort;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Alta de usuarios finales. El cliente nunca elige id, {@code activated},
 * autoridades ni el {@code externalPatientId}: los cuatro los decide este
 * caso de uso.
 */
@Component
@RequiredArgsConstructor
public class RegistrarUsuarioUseCaseImpl implements RegistrarUsuarioUseCase {

    /** Unica autoridad que existe hoy. El rol administrador queda fuera de alcance de #9. */
    private static final String ROL_USUARIO = "ROLE_USER";

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    @Override
    public Usuario registrar(SolicitudRegistroUsuario solicitud) {
        // login y email se normalizan a minusculas antes de comparar y de
        // guardar (igual que JHipster): asi la unicidad es case-insensitive
        // sin necesitar un indice funcional en la base.
        String login = solicitud.getLogin().toLowerCase(Locale.ROOT);
        String email = solicitud.getEmail().toLowerCase(Locale.ROOT);

        if (usuarioRepositoryPort.existeConLogin(login)) {
            throw new UsuarioDuplicadoException(UsuarioDuplicadoException.Campo.LOGIN, login);
        }
        if (usuarioRepositoryPort.existeConEmail(email)) {
            throw new UsuarioDuplicadoException(UsuarioDuplicadoException.Campo.EMAIL, email);
        }

        Usuario usuario = Usuario.builder()
                .externalPatientId(UUID.randomUUID().toString())
                .login(login)
                .passwordHash(passwordHasherPort.hashear(solicitud.getPassword()))
                .firstName(solicitud.getFirstName())
                .lastName(solicitud.getLastName())
                .email(email)
                .imageUrl(solicitud.getImageUrl())
                .langKey(solicitud.getLangKey())
                .activated(true)
                .authorities(Set.of(ROL_USUARIO))
                .build();

        return usuarioRepositoryPort.guardar(usuario);
    }

}

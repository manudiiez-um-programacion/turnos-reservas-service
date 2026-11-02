package ar.edu.um.turnos.auth.application.usecases;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.application.exception.CredencialesInvalidasException;
import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.AutenticarUsuarioUseCase;
import ar.edu.um.turnos.auth.domain.ports.out.JwtIssuerPort;
import ar.edu.um.turnos.auth.domain.ports.out.PasswordHasherPort;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Login de usuarios finales. No distingue, ni en el resultado ni en el
 * tiempo que tarda en fallar antes de verificar el hash, si el login no
 * existe o si la contrasena es incorrecta: las dos terminan en la misma
 * {@link CredencialesInvalidasException}.
 */
@Component
@RequiredArgsConstructor
public class AutenticarUsuarioUseCaseImpl implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final JwtIssuerPort jwtIssuerPort;

    @Override
    public String autenticar(SolicitudAutenticacion solicitud) {
        Objects.requireNonNull(solicitud, "la solicitud de autenticacion no puede ser null");

        String login = solicitud.getLogin().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepositoryPort.buscarPorLogin(login)
                .filter(Usuario::isActivated)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordHasherPort.verificar(solicitud.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        return jwtIssuerPort.emitir(usuario);
    }

}

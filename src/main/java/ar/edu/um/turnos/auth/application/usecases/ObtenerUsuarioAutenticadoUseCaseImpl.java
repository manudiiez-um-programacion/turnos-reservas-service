package ar.edu.um.turnos.auth.application.usecases;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.application.exception.UsuarioNoEncontradoException;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.ObtenerUsuarioAutenticadoUseCase;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ObtenerUsuarioAutenticadoUseCaseImpl implements ObtenerUsuarioAutenticadoUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    @Override
    public Usuario obtenerPorLogin(String login) {
        return usuarioRepositoryPort.buscarPorLogin(login)
                .orElseThrow(() -> new UsuarioNoEncontradoException(login));
    }

}

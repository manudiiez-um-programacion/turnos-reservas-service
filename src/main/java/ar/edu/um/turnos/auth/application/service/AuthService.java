package ar.edu.um.turnos.auth.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;
import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.AutenticarUsuarioUseCase;
import ar.edu.um.turnos.auth.domain.ports.in.RegistrarUsuarioUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice {@code auth}: es lo unico que la capa web conoce. El
 * controlador no habla con un caso de uso ni con un puerto de salida
 * directamente.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    public Usuario registrar(SolicitudRegistroUsuario solicitud) {
        return registrarUsuarioUseCase.registrar(solicitud);
    }

    public String autenticar(SolicitudAutenticacion solicitud) {
        return autenticarUsuarioUseCase.autenticar(solicitud);
    }

}

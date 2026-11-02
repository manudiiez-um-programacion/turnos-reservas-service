package ar.edu.um.turnos.auth.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.in.RegistrarUsuarioUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice: es lo unico que la capa web conoce. El controlador no
 * habla con un caso de uso ni con un puerto de salida directamente.
 */
@Service
@RequiredArgsConstructor
public class RegistroService {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;

    public Usuario registrar(SolicitudRegistroUsuario solicitud) {
        return registrarUsuarioUseCase.registrar(solicitud);
    }

}

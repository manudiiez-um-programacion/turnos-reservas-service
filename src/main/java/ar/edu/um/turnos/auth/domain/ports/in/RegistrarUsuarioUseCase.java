package ar.edu.um.turnos.auth.domain.ports.in;

import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.domain.model.Usuario;

/**
 * Alta de un usuario final (#9). El usuario queda activo de inmediato: no
 * hay verificacion por correo en el alcance de este servicio.
 */
public interface RegistrarUsuarioUseCase {

    Usuario registrar(SolicitudRegistroUsuario solicitud);

}

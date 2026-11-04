package ar.edu.um.turnos.auth.domain.ports.in;

import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;

/**
 * Login de un usuario final (#10). Devuelve el JWT de usuario ya firmado;
 * credenciales invalidas se senalan con
 * {@link ar.edu.um.turnos.auth.application.exception.CredencialesInvalidasException},
 * sin distinguir si el login no existe o si la contrasena es incorrecta.
 */
public interface AutenticarUsuarioUseCase {

    String autenticar(SolicitudAutenticacion solicitud);

}

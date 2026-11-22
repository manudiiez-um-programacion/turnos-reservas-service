package ar.edu.um.turnos.auth.domain.ports.in;

import ar.edu.um.turnos.auth.domain.model.Usuario;

/**
 * Puerto de entrada: resuelve el {@link Usuario} dueno del {@code login} que
 * llego en el claim {@code sub} del JWT (#27 y siguientes, §4.2: la
 * asociacion con el usuario final nunca se confia desde el cliente). Lo usan
 * los controladores de otros slices que necesitan el {@code id} interno o el
 * {@code externalPatientId} del usuario autenticado, sin que esos slices
 * hablen directo con {@code UsuarioRepositoryPort}.
 */
public interface ObtenerUsuarioAutenticadoUseCase {

    /**
     * @throws ar.edu.um.turnos.auth.application.exception.UsuarioNoEncontradoException
     *         si el {@code login} del token ya no corresponde a ningun
     *         usuario (por ejemplo, borrado despues de emitirse el JWT).
     */
    Usuario obtenerPorLogin(String login);

}

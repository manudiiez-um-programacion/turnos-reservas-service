package ar.edu.um.turnos.auth.domain.ports.out;

import java.util.Optional;
import ar.edu.um.turnos.auth.domain.model.Usuario;

/**
 * Puerto de salida: persistencia de usuarios finales. La implementacion vive
 * en {@code infrastructure/persistence/adapter} y delega en un
 * {@code JpaRepository}.
 */
public interface UsuarioRepositoryPort {

    boolean existeConLogin(String login);

    boolean existeConEmail(String email);

    Optional<Usuario> buscarPorLogin(String login);

    Usuario guardar(Usuario usuario);

}

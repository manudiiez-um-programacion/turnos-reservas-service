package ar.edu.um.turnos.auth.domain.ports.out;

/**
 * Puerto de salida: hasheo de contrasenas. El dominio no sabe que algoritmo
 * se usa (Argon2id, hoy) ni con que parametros: eso es un detalle de
 * {@code infrastructure}, adaptado desde el {@code PasswordEncoder} de
 * Spring Security.
 */
public interface PasswordHasherPort {

    String hashear(String passwordEnTextoPlano);

    boolean verificar(String passwordEnTextoPlano, String hash);

}

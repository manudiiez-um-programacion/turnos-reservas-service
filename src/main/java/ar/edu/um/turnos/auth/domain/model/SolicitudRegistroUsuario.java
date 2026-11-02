package ar.edu.um.turnos.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Datos de entrada del caso de uso de registro, ya validados en el borde web.
 *
 * <p>No es el DTO HTTP: ese vive en {@code infrastructure/web/dto} y se
 * mapea a esto antes de llegar al dominio. Lleva la contrasena en texto
 * plano porque todavia no se hasheo; el caso de uso es quien pide el hash a
 * traves de {@link ar.edu.um.turnos.auth.domain.ports.out.PasswordHasherPort}
 * y nunca la persiste ni la loguea como llego.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRegistroUsuario {

    private String login;

    private String password;

    private String firstName;

    private String lastName;

    private String email;

    private String imageUrl;

    private String langKey;

}

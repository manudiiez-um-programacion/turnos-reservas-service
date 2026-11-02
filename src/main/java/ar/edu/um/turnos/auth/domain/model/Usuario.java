package ar.edu.um.turnos.auth.domain.model;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario final de la app KMP. Vive solo en este servicio (ADR 0002 D1 del
 * repo de catalogo): el de catalogo no tiene usuarios ni login.
 *
 * <p>No tiene anotaciones de framework a proposito: es el objeto que circula
 * por {@code application} y por los puertos, nunca la {@code @Entity} de JPA
 * ni un DTO web.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    private Long id;

    /** Identificador estable y opaco que se envia a la catedra como {@code externalPatientId}. */
    private String externalPatientId;

    private String login;

    /** Hash de la contrasena (Argon2id, prefijo {@code {argon2}}). Nunca texto plano. */
    private String passwordHash;

    private String firstName;

    private String lastName;

    private String email;

    private String imageUrl;

    private String langKey;

    private boolean activated;

    private Set<String> authorities;

}

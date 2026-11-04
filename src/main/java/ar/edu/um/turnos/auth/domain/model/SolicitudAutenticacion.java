package ar.edu.um.turnos.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Credenciales de entrada al caso de uso de autenticacion, ya validadas en
 * el borde web. Lleva la contrasena en texto plano porque todavia no se
 * verifico contra el hash guardado.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAutenticacion {

    private String login;

    private String password;

}

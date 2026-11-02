package ar.edu.um.turnos.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Secretos y vigencias de los JWT que <strong>emite este servicio</strong>.
 *
 * <p>Son dos credenciales distintas y no se mezclan nunca:
 *
 * <ul>
 *   <li><strong>JWT de usuario</strong> ({@code userSecret}): lo recibe el
 *       usuario final al autenticarse. Lo emite este servicio, que es el dueño
 *       de los usuarios, y tambien lo valida el servicio de catalogo, porque la
 *       app KMP le consulta la busqueda directo a el.</li>
 *   <li><strong>JWT tecnico interno</strong> ({@code internalSecret}): lo usa
 *       este servicio para llamar al de catalogo, con autoridad
 *       {@code SERVICE_TURNOS}. No representa a ningun usuario.</li>
 * </ul>
 *
 * <p>Que los dos secretos sean <strong>distintos</strong> no es una convencion
 * ordenada: es lo que hace <em>criptografica</em> la separacion entre las dos
 * identidades. Con un secreto compartido, un token de usuario robado se podria
 * presentar como si fuera el del servicio, y la frontera entre "un usuario
 * pidiendo algo" y "un backend pidiendo algo" dejaria de existir. Por eso hay
 * una validacion que rechaza el arranque si coinciden.
 *
 * <p>El JWT tecnico <em>de la catedra</em> es una tercera credencial y no esta
 * aca: vive en {@link CatedraProperties} y solo va hacia afuera, hacia el
 * servicio central. Nunca se acepta como credencial de entrada ni se reenvia.
 *
 * @param issuer             valor del claim {@code iss} de los tokens emitidos
 * @param userSecret         secreto HS256 del JWT de usuario. <strong>Secreto.</strong>
 * @param internalSecret     secreto HS256 del JWT tecnico interno. <strong>Secreto.</strong>
 * @param userExpiration     vigencia del JWT de usuario
 * @param internalExpiration vigencia del JWT tecnico interno
 */
@Validated
@ConfigurationProperties(prefix = "turnos.jwt")
public record JwtProperties(

        @NotBlank(message = "falta JWT_ISSUER")
        String issuer,

        @NotBlank(message = "falta JWT_USER_SECRET")
        @Size(min = MINIMO_HS256, message = "JWT_USER_SECRET necesita al menos {min} caracteres para HS256")
        String userSecret,

        @NotBlank(message = "falta JWT_INTERNAL_SECRET")
        @Size(min = MINIMO_HS256, message = "JWT_INTERNAL_SECRET necesita al menos {min} caracteres para HS256")
        String internalSecret,

        @NotNull(message = "falta JWT_USER_EXPIRATION")
        Duration userExpiration,

        @NotNull(message = "falta JWT_INTERNAL_EXPIRATION")
        Duration internalExpiration

) {

    /**
     * Largo minimo de un secreto HS256.
     *
     * <p>HS256 firma con HMAC-SHA256, cuyo bloque es de 256 bits. Un secreto
     * mas corto que eso no aporta la entropia que el algoritmo supone y las
     * librerias de JWT lo rechazan en tiempo de ejecucion. Se mide en
     * caracteres y no en bytes a proposito: para un secreto ASCII coinciden, y
     * para uno con caracteres multibyte el limite queda del lado conservador.
     */
    static final int MINIMO_HS256 = 32;

    /**
     * Rechaza el arranque si los dos secretos coinciden.
     *
     * <p>Es el error facil de cometer al armar el {@code .env}: copiar el mismo
     * valor en las dos variables deja todo funcionando en apariencia y elimina
     * en silencio la separacion entre la identidad del usuario y la del
     * servicio. Se prefiere no arrancar.
     */
    @AssertTrue(message = "JWT_USER_SECRET y JWT_INTERNAL_SECRET tienen que ser DISTINTOS: "
            + "un secreto compartido borra la separacion entre la identidad del usuario y la del servicio")
    public boolean isSecretosDistintos() {
        if (userSecret == null || internalSecret == null) {
            // Ausentes: ya lo reportan las validaciones de cada campo. Devolver
            // false aca solo agregaria un mensaje confuso a ese error.
            return true;
        }
        return !Objects.equals(userSecret, internalSecret);
    }

    /** No imprime ninguno de los dos secretos. */
    @Override
    public String toString() {
        return "JwtProperties[issuer=%s, userSecret=%s, internalSecret=%s, userExpiration=%s, internalExpiration=%s]"
                .formatted(issuer,
                        SecretosEnmascarados.enmascarar(userSecret),
                        SecretosEnmascarados.enmascarar(internalSecret),
                        userExpiration, internalExpiration);
    }

}

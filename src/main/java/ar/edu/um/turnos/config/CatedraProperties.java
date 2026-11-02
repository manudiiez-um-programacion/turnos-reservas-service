package ar.edu.um.turnos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion de la integracion con el servicio central de la catedra.
 *
 * <p>Los valores los entrega la catedra y <strong>no se fijan en el codigo ni
 * se versionan</strong> (§3 de la referencia de integracion): llegan por
 * variables de entorno desde el {@code .env}, del que solo se versiona el
 * {@code .env.example}. De donde salen:
 *
 * <ul>
 *   <li>{@code groupId}, Redis y Kafka vienen en el objeto {@code integration}
 *       que devuelve {@code GET /api/student/integration} (§5.3);</li>
 *   <li>el {@code jwt} tecnico lo emite el registro y lo renueva
 *       {@code POST /api/authenticate} (§5.1 y §5.2). Vigencia: un año;</li>
 *   <li>la URL base de la API la informa la catedra por otro canal (§3).</li>
 * </ul>
 *
 * <p>Son un {@code record} validado: si falta una propiedad obligatoria, el
 * contexto <strong>no arranca</strong> y el error dice exactamente cual. Es
 * preferible a descubrir en la primera llamada a la catedra que el servicio
 * venia corriendo a medias.
 *
 * <p>Los nombres de los topics y el consumer group dependen del
 * {@code groupId}, asi que son configurables y nunca se arman concatenando en
 * el codigo: si la catedra reprovisiona la cuenta, cambia el {@code .env} y no
 * hay que recompilar.
 *
 * <p><strong>El JWT tecnico no sale de este servicio hacia ningun cliente</strong>
 * (§2 de la referencia): no se envia a la app KMP ni al servicio de catalogo, y
 * no se acepta como credencial de entrada. Las llamadas al catalogo usan el JWT
 * interno de {@link JwtProperties}.
 *
 * @param groupId identificador del proyecto en la catedra; es el sufijo de los
 *                topics y del consumer group
 * @param jwt     JWT tecnico de la cuenta del proyecto. <strong>Secreto.</strong>
 * @param api     acceso REST al servicio central
 * @param kafka   topics y consumer group del intercambio asincronico
 * @param redis   acceso a Redis; opcional para este servicio (ver {@link Redis})
 */
@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(

        @NotBlank(message = "falta CATEDRA_GROUP_ID: lo devuelve GET /api/student/integration")
        String groupId,

        @NotBlank(message = "falta CATEDRA_JWT: el JWT tecnico que emite el registro de la cuenta")
        String jwt,

        @NotNull @Valid Api api,

        @NotNull @Valid Kafka kafka,

        @Valid Redis redis

) {

    /**
     * Acceso REST al servicio central.
     *
     * @param baseUrl URL base de la API, sin barra final
     */
    public record Api(
            @NotBlank(message = "falta CATEDRA_API_BASE_URL: la informa la catedra por otro canal")
            String baseUrl
    ) {
    }

    /**
     * Topics y consumer group del intercambio asincronico.
     *
     * <p>Los dos topics son los de <em>turnos</em>. El topic de catalogo
     * ({@code catedra.catalog.{groupId}}, con {@code CatalogUpdated}) es del
     * servicio de catalogo y a proposito no esta aca: este servicio no lo
     * consume.
     *
     * @param bootstrapServers lista de brokers, formato {@code host:puerto[,host:puerto]}
     * @param consumerGroupId  consumer group de la cuenta tecnica
     * @param appointmentActionsTopic topic de entrada: {@code alumnos.turnos.acciones.{groupId}}
     * @param appointmentPhoneTopic   topic de salida: {@code catedra.turnos.telefono.{groupId}}
     */
    public record Kafka(
            @NotBlank(message = "falta CATEDRA_KAFKA_BOOTSTRAP_SERVERS")
            String bootstrapServers,

            @NotBlank(message = "falta CATEDRA_KAFKA_CONSUMER_GROUP_ID")
            String consumerGroupId,

            @NotBlank(message = "falta CATEDRA_KAFKA_APPOINTMENT_ACTIONS_TOPIC (catedra -> alumno)")
            String appointmentActionsTopic,

            @NotBlank(message = "falta CATEDRA_KAFKA_APPOINTMENT_PHONE_TOPIC (alumno -> catedra)")
            String appointmentPhoneTopic
    ) {
    }

    /**
     * Acceso a Redis. <strong>Opcional para este servicio.</strong>
     *
     * <p>Redis es como la catedra publica el snapshot del catalogo, y eso lo
     * consume el <em>otro</em> servicio. Este no lo necesita: las ocupaciones y
     * los holds se piden por REST, y §14.1 es explicito en que los holds no
     * tienen acceso contractual por Redis. El unico uso posible aca seria el
     * namespace privado {@code alumnos:{groupId}:*} para estado auxiliar
     * (§14.6), que ademas no es fuente autoritativa de nada.
     *
     * <p>Por eso el bloque entero se valida solo si esta presente y sus campos
     * no son obligatorios: exigir credenciales de Redis para arrancar seria
     * pedir configuracion que el servicio hoy no usa. Si alguna vez se usa el
     * namespace privado, la obligatoriedad se agrega en ese issue, no antes.
     *
     * @param host           host de Redis
     * @param port           puerto de Redis
     * @param username       usuario de la ACL de la cuenta tecnica
     * @param password       password de Redis. <strong>Secreto.</strong>
     * @param readNamespace  namespace de solo lectura ({@code catedra:sync:*})
     * @param writeNamespace namespace propio de lectura y escritura
     */
    public record Redis(
            String host,

            @Positive @Max(65535)
            Integer port,

            String username,

            String password,

            String readNamespace,

            String writeNamespace
    ) {

        /** No imprime la password. */
        @Override
        public String toString() {
            return "Redis[host=%s, port=%s, username=%s, password=%s, readNamespace=%s, writeNamespace=%s]"
                    .formatted(host, port, username,
                            SecretosEnmascarados.enmascarar(password),
                            readNamespace, writeNamespace);
        }

    }

    /** No imprime el JWT tecnico. */
    @Override
    public String toString() {
        return "CatedraProperties[groupId=%s, jwt=%s, api=%s, kafka=%s, redis=%s]"
                .formatted(groupId, SecretosEnmascarados.enmascarar(jwt), api, kafka, redis);
    }

}

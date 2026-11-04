package ar.edu.um.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/**
 * Verifica que la configuracion de integracion sea obligatoria y que el JWT
 * tecnico no se filtre por un log.
 *
 * <p>Se usa {@link ApplicationContextRunner} y no {@code @SpringBootTest}
 * porque lo que se prueba es justamente el <em>arranque</em>: hace falta poder
 * levantar el contexto con configuracion incompleta y comprobar que falla, algo
 * que un contexto compartido y ya valido no permite observar.
 */
@DisplayName("CatedraProperties")
class CatedraPropertiesTest {

    /** Configuracion completa y valida, de la que cada prueba quita una pieza. */
    private static final String[] CONFIGURACION_COMPLETA = {
        "catedra.group-id=proyecto-de-prueba",
        "catedra.jwt=jwt-tecnico-de-prueba-no-es-una-credencial",
        "catedra.api.base-url=http://catedra.invalido:9999",
        "catedra.api.connect-timeout=PT2S",
        "catedra.api.read-timeout=PT5S",
        "catedra.api.max-retries=3",
        "catedra.api.retry-backoff=PT0.2S",
        "catedra.kafka.bootstrap-servers=kafka.invalido:9092",
        "catedra.kafka.consumer-group-id=alumnos-proyecto-de-prueba",
        "catedra.kafka.appointment-actions-topic=alumnos.turnos.acciones.proyecto-de-prueba",
        "catedra.kafka.appointment-phone-topic=catedra.turnos.telefono.proyecto-de-prueba",
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(HabilitarPropiedades.class);

    @EnableConfigurationProperties(CatedraProperties.class)
    static class HabilitarPropiedades {
    }

    @Test
    @DisplayName("con la configuracion completa, liga todos los valores")
    void ligaLaConfiguracionCompleta() {
        runner.withPropertyValues(CONFIGURACION_COMPLETA).run(context -> {
            assertThat(context).hasNotFailed();
            CatedraProperties props = context.getBean(CatedraProperties.class);

            assertThat(props.groupId()).isEqualTo("proyecto-de-prueba");
            assertThat(props.api().baseUrl()).isEqualTo("http://catedra.invalido:9999");
            assertThat(props.kafka().appointmentActionsTopic())
                    .isEqualTo("alumnos.turnos.acciones.proyecto-de-prueba");
            assertThat(props.kafka().appointmentPhoneTopic())
                    .isEqualTo("catedra.turnos.telefono.proyecto-de-prueba");
        });
    }

    @Nested
    @DisplayName("falla al arrancar")
    class FallaAlArrancar {

        @Test
        @DisplayName("si no hay ninguna configuracion de catedra")
        void siNoHayConfiguracion() {
            runner.run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si falta el JWT tecnico")
        void siFaltaElJwt() {
            runner.withPropertyValues(sinLaPropiedad("catedra.jwt"))
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si falta la URL base de la API")
        void siFaltaLaUrlBase() {
            runner.withPropertyValues(sinLaPropiedad("catedra.api.base-url"))
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si falta el groupId")
        void siFaltaElGroupId() {
            runner.withPropertyValues(sinLaPropiedad("catedra.group-id"))
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si falta un topic de Kafka")
        void siFaltaUnTopic() {
            runner.withPropertyValues(sinLaPropiedad("catedra.kafka.appointment-phone-topic"))
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si una propiedad obligatoria esta presente pero vacia")
        void siUnaPropiedadEstaVacia() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .withPropertyValues("catedra.jwt=")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Nested
    @DisplayName("Redis es opcional")
    class RedisEsOpcional {

        @Test
        @DisplayName("arranca sin ninguna propiedad de Redis")
        void arrancaSinRedis() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        @DisplayName("pero un puerto fuera de rango se rechaza")
        void rechazaUnPuertoInvalido() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .withPropertyValues("catedra.redis.port=70000")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Nested
    @DisplayName("no filtra secretos por toString")
    class NoFiltraSecretos {

        private static final String JWT = "jwt-tecnico-de-prueba-no-es-una-credencial";
        private static final String PASSWORD_REDIS = "password-de-redis-de-prueba";

        @Test
        @DisplayName("el JWT tecnico queda enmascarado")
        void enmascaraElJwt() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA).run(context -> {
                String impreso = context.getBean(CatedraProperties.class).toString();

                assertThat(impreso).doesNotContain(JWT);
                // Sigue siendo diagnosticable: se ve el prefijo y el largo.
                assertThat(impreso).contains("jwt-***");
            });
        }

        @Test
        @DisplayName("la password de Redis queda enmascarada")
        void enmascaraLaPasswordDeRedis() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .withPropertyValues("catedra.redis.password=" + PASSWORD_REDIS)
                    .run(context -> {
                        String impreso = context.getBean(CatedraProperties.class).toString();

                        assertThat(impreso).doesNotContain(PASSWORD_REDIS);
                        assertThat(impreso).contains("pass***");
                    });
        }

        @Test
        @DisplayName("lo no secreto si se imprime, para poder diagnosticar")
        void imprimeLoNoSecreto() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA).run(context -> {
                String impreso = context.getBean(CatedraProperties.class).toString();

                assertThat(impreso)
                        .contains("proyecto-de-prueba")
                        .contains("kafka.invalido:9092");
            });
        }
    }

    /** La configuracion completa menos una propiedad, para probar que es obligatoria. */
    private static String[] sinLaPropiedad(String clave) {
        return java.util.Arrays.stream(CONFIGURACION_COMPLETA)
                .filter(par -> !par.startsWith(clave + "="))
                .toArray(String[]::new);
    }

}

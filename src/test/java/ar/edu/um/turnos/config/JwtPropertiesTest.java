package ar.edu.um.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/**
 * Verifica las reglas de los JWT que emite este servicio: secretos presentes,
 * suficientemente largos, <strong>distintos entre si</strong> y nunca impresos.
 */
@DisplayName("JwtProperties")
class JwtPropertiesTest {

    private static final String SECRETO_USUARIO = "secreto-de-usuario-solo-para-pruebas-0001";
    private static final String SECRETO_INTERNO = "secreto-interno-solo-para-pruebas-0002";

    private static final String[] CONFIGURACION_COMPLETA = {
        "turnos.jwt.issuer=turnos-reservas-service-test",
        "turnos.jwt.user-secret=" + SECRETO_USUARIO,
        "turnos.jwt.internal-secret=" + SECRETO_INTERNO,
        "turnos.jwt.user-expiration=PT1H",
        "turnos.jwt.internal-expiration=PT2M",
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(HabilitarPropiedades.class);

    @EnableConfigurationProperties(JwtProperties.class)
    static class HabilitarPropiedades {
    }

    @Test
    @DisplayName("con la configuracion completa, liga los valores y las vigencias")
    void ligaLaConfiguracionCompleta() {
        runner.withPropertyValues(CONFIGURACION_COMPLETA).run(context -> {
            assertThat(context).hasNotFailed();
            JwtProperties props = context.getBean(JwtProperties.class);

            assertThat(props.issuer()).isEqualTo("turnos-reservas-service-test");
            assertThat(props.userSecret()).isEqualTo(SECRETO_USUARIO);
            assertThat(props.userExpiration()).isEqualTo(Duration.ofHours(1));
            assertThat(props.internalExpiration()).isEqualTo(Duration.ofMinutes(2));
        });
    }

    @Nested
    @DisplayName("separacion entre las dos identidades")
    class SeparacionDeIdentidades {

        @Test
        @DisplayName("rechaza el arranque si los dos secretos son iguales")
        void rechazaSecretosIguales() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .withPropertyValues("turnos.jwt.internal-secret=" + SECRETO_USUARIO)
                    .run(context -> assertThat(context)
                            .hasFailed()
                            .getFailure()
                            .hasStackTraceContaining("DISTINTOS"));
        }

        @Test
        @DisplayName("acepta dos secretos distintos")
        void aceptaSecretosDistintos() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .run(context -> assertThat(context).hasNotFailed());
        }
    }

    @Nested
    @DisplayName("falla al arrancar")
    class FallaAlArrancar {

        @Test
        @DisplayName("si no hay ninguna configuracion de JWT")
        void siNoHayConfiguracion() {
            runner.run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si falta el secreto de usuario")
        void siFaltaElSecretoDeUsuario() {
            runner.withPropertyValues(
                            "turnos.jwt.issuer=turnos-reservas-service-test",
                            "turnos.jwt.internal-secret=" + SECRETO_INTERNO,
                            "turnos.jwt.user-expiration=PT1H",
                            "turnos.jwt.internal-expiration=PT2M")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("si un secreto es mas corto que lo que HS256 necesita")
        void siUnSecretoEsDemasiadoCorto() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .withPropertyValues("turnos.jwt.user-secret=corto")
                    .run(context -> assertThat(context)
                            .hasFailed()
                            .getFailure()
                            .hasStackTraceContaining("HS256"));
        }

        @Test
        @DisplayName("si una vigencia no es una duracion valida")
        void siUnaVigenciaEsInvalida() {
            runner.withPropertyValues(CONFIGURACION_COMPLETA)
                    .withPropertyValues("turnos.jwt.user-expiration=un-rato")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    @DisplayName("no filtra ninguno de los dos secretos por toString")
    void noFiltraLosSecretos() {
        runner.withPropertyValues(CONFIGURACION_COMPLETA).run(context -> {
            String impreso = context.getBean(JwtProperties.class).toString();

            assertThat(impreso)
                    .doesNotContain(SECRETO_USUARIO)
                    .doesNotContain(SECRETO_INTERNO);
            // El issuer y las vigencias no son secretos y si se imprimen.
            assertThat(impreso)
                    .contains("turnos-reservas-service-test")
                    .contains("PT1H");
        });
    }

}

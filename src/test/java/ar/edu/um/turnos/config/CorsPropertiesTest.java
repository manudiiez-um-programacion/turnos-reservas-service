package ar.edu.um.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/**
 * Verifica que, sin configuracion, ningun origen cruzado quede permitido, y
 * que el comodin {@code *} rechace el arranque (#11, AC: "lista explicita de
 * origenes permitidos, no *").
 */
@DisplayName("CorsProperties")
class CorsPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(HabilitarPropiedades.class);

    @EnableConfigurationProperties(CorsProperties.class)
    static class HabilitarPropiedades {
    }

    @Test
    @DisplayName("sin configuracion, no permite ningun origen")
    void sinConfiguracionNoPermiteNingunOrigen() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CorsProperties.class).allowedOrigins()).isEmpty();
        });
    }

    @Test
    @DisplayName("liga una lista de origenes explicitos")
    void ligaUnaListaDeOrigenes() {
        runner.withPropertyValues(
                        "turnos.cors.allowed-origins=https://app.ejemplo.com,https://otro.ejemplo.com")
                .run(context -> assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                        .containsExactly("https://app.ejemplo.com", "https://otro.ejemplo.com"));
    }

    @Nested
    @DisplayName("rechaza el comodin")
    class RechazaElComodin {

        @Test
        @DisplayName("si la lista es solo '*'")
        void siLaListaEsSoloComodin() {
            runner.withPropertyValues("turnos.cors.allowed-origins=*")
                    .run(context -> assertThat(context)
                            .hasFailed()
                            .getFailure()
                            .hasStackTraceContaining("no puede contener"));
        }

        @Test
        @DisplayName("si el comodin aparece junto con origenes concretos")
        void siElComodinApareceMezclado() {
            runner.withPropertyValues("turnos.cors.allowed-origins=https://app.ejemplo.com,*")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    @DisplayName("la lista no se puede modificar desde afuera")
    void laListaEsInmutable() {
        CorsProperties props = new CorsProperties(List.of("https://app.ejemplo.com"));

        assertThat(props.allowedOrigins()).hasSize(1);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> props.allowedOrigins().add("https://otro.com"));
    }

}

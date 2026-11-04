package ar.edu.um.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo: verifica que el contexto de Spring se levanta completo.
 *
 * <p>Detecta los errores de cableado (beans faltantes, dependencias ciclicas,
 * configuracion invalida) que de otro modo solo aparecerian al arrancar a mano.
 *
 * <p>El trabajo real lo hace {@code @SpringBootTest} antes de entrar al metodo:
 * si el contexto no arranca, la prueba falla ahi. La asercion sobre el contexto
 * inyectado deja esa expectativa escrita en lugar de dejar el cuerpo vacio, que
 * no se distingue de una prueba a medio escribir.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TurnosReservasApplication")
class TurnosReservasApplicationTests {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("levanta el contexto de la aplicacion")
    void elContextoSeLevanta() {
        assertThat(contexto).isNotNull();
        assertThat(contexto.getBean(TurnosReservasApplication.class)).isNotNull();
    }

}

package ar.edu.um.turnos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo: verifica que el contexto de Spring se levanta completo.
 *
 * <p>Detecta los errores de cableado (beans faltantes, dependencias ciclicas,
 * configuracion invalida) que de otro modo solo aparecerian al arrancar a mano.
 */
@SpringBootTest
@ActiveProfiles("test")
class TurnosReservasApplicationTests {

    @Test
    void elContextoSeLevanta() {
    }

}

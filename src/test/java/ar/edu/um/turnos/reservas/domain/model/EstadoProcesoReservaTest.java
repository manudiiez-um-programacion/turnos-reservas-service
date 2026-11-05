package ar.edu.um.turnos.reservas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Cubre la maquina de estados del proceso de reserva (#26, §7 del
 * enunciado): las transiciones que el flujo necesita, y que ninguna otra se
 * aplique silenciosamente.
 */
@DisplayName("EstadoProcesoReserva")
class EstadoProcesoReservaTest {

    static Stream<Arguments> transicionesValidasDelFlujo() {
        return Stream.of(
                Arguments.of(EstadoProcesoReserva.HOLD_CREADO, EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA),
                Arguments.of(EstadoProcesoReserva.HOLD_CREADO, EstadoProcesoReserva.VENCIDA),
                Arguments.of(EstadoProcesoReserva.HOLD_CREADO, EstadoProcesoReserva.INVALIDA),
                Arguments.of(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA, EstadoProcesoReserva.ESPERANDO_TELEFONO),
                Arguments.of(EstadoProcesoReserva.ESPERANDO_TELEFONO, EstadoProcesoReserva.TELEFONO_ENVIADO),
                Arguments.of(EstadoProcesoReserva.TELEFONO_ENVIADO, EstadoProcesoReserva.CONFIRMADA),
                Arguments.of(EstadoProcesoReserva.TELEFONO_ENVIADO, EstadoProcesoReserva.TELEFONO_RECHAZADO),
                Arguments.of(EstadoProcesoReserva.TELEFONO_RECHAZADO, EstadoProcesoReserva.TELEFONO_ENVIADO),
                Arguments.of(EstadoProcesoReserva.TELEFONO_RECHAZADO, EstadoProcesoReserva.VENCIDA),
                Arguments.of(EstadoProcesoReserva.CONFIRMADA, EstadoProcesoReserva.CANCELADA));
    }

    @ParameterizedTest(name = "{0} puede transicionar a {1}")
    @MethodSource("transicionesValidasDelFlujo")
    void permiteLasTransicionesDelFlujo(EstadoProcesoReserva origen, EstadoProcesoReserva destino) {
        assertThat(origen.puedeTransicionarA(destino)).isTrue();
    }

    @ParameterizedTest(name = "{0} no puede retroceder a un estado anterior")
    @EnumSource(
            value = EstadoProcesoReserva.class,
            names = {"CONFIRMADA", "VENCIDA", "INVALIDA", "CANCELADA"})
    void losEstadosFinalesNoRetrocedenAUnEstadoIntermedio(EstadoProcesoReserva estadoFinal) {
        EnumSet<EstadoProcesoReserva> estadosIntermedios = EnumSet.of(
                EstadoProcesoReserva.HOLD_CREADO,
                EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA,
                EstadoProcesoReserva.ESPERANDO_TELEFONO,
                EstadoProcesoReserva.TELEFONO_ENVIADO,
                EstadoProcesoReserva.TELEFONO_RECHAZADO);

        for (EstadoProcesoReserva destino : estadosIntermedios) {
            assertThat(estadoFinal.puedeTransicionarA(destino))
                    .as("%s -> %s deberia estar prohibido", estadoFinal, destino)
                    .isFalse();
        }
    }

    @ParameterizedTest(name = "{0} no tiene ninguna transicion valida (totalmente terminal)")
    @EnumSource(value = EstadoProcesoReserva.class, names = {"VENCIDA", "INVALIDA", "CANCELADA"})
    void losEstadosTerminalesNoTienenNingunaTransicionValida(EstadoProcesoReserva estadoTerminal) {
        for (EstadoProcesoReserva destino : EstadoProcesoReserva.values()) {
            assertThat(estadoTerminal.puedeTransicionarA(destino)).isFalse();
        }
    }

    @ParameterizedTest(name = "CONFIRMADA no puede transicionar a {0}")
    @EnumSource(
            value = EstadoProcesoReserva.class,
            names = {"CONFIRMADA", "CANCELADA"},
            mode = EnumSource.Mode.EXCLUDE)
    void confirmadaSoloPuedeTransicionarACancelada(EstadoProcesoReserva destino) {
        assertThat(EstadoProcesoReserva.CONFIRMADA.puedeTransicionarA(destino)).isFalse();
    }

    @ParameterizedTest(name = "{0} es un estado final")
    @EnumSource(
            value = EstadoProcesoReserva.class,
            names = {"CONFIRMADA", "VENCIDA", "INVALIDA", "CANCELADA"})
    void marcaComoFinalLosCuatroEstadosDelEnunciado(EstadoProcesoReserva estado) {
        assertThat(estado.esFinal()).isTrue();
    }

    @ParameterizedTest(name = "{0} no es un estado final")
    @EnumSource(
            value = EstadoProcesoReserva.class,
            names = {"HOLD_CREADO", "CONFIRMACION_INICIAL_ENVIADA", "ESPERANDO_TELEFONO", "TELEFONO_ENVIADO", "TELEFONO_RECHAZADO"})
    void noMarcaComoFinalLosEstadosIntermedios(EstadoProcesoReserva estado) {
        assertThat(estado.esFinal()).isFalse();
    }

}

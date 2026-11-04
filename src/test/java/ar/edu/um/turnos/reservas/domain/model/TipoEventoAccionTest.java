package ar.edu.um.turnos.reservas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Cubre el mapeo de {@link TipoEventoAccion#desde(String)} contra los seis
 * {@code eventType} de {@code alumnos.turnos.acciones.{groupId}} (§15.4-§15.10
 * de la referencia).
 */
@DisplayName("TipoEventoAccion")
class TipoEventoAccionTest {

    static Stream<Arguments> eventTypesDelContrato() {
        return Stream.of(
                Arguments.of("AdditionalInformationRequested", TipoEventoAccion.SOLICITUD_INFORMACION_ADICIONAL),
                Arguments.of("AppointmentConfirmed", TipoEventoAccion.RESERVA_CONFIRMADA),
                Arguments.of("AdditionalInformationRejected", TipoEventoAccion.INFORMACION_ADICIONAL_RECHAZADA),
                Arguments.of("AppointmentProcessExpired", TipoEventoAccion.PROCESO_VENCIDO),
                Arguments.of("AppointmentProcessInvalid", TipoEventoAccion.PROCESO_INVALIDO),
                Arguments.of("AppointmentCancelled", TipoEventoAccion.RESERVA_CANCELADA));
    }

    @ParameterizedTest(name = "{0} se mapea a {1}")
    @MethodSource("eventTypesDelContrato")
    void mapeaCadaEventTypeDelContrato(String eventType, TipoEventoAccion esperado) {
        assertThat(TipoEventoAccion.desde(eventType)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "''{0}'' se mapea a DESCONOCIDO")
    @MethodSource("eventTypesSinMapeo")
    void mapeaAlResguardoCuandoNoHayMapeoEspecifico(String eventType) {
        assertThat(TipoEventoAccion.desde(eventType)).isEqualTo(TipoEventoAccion.DESCONOCIDO);
    }

    static Stream<String> eventTypesSinMapeo() {
        return Stream.of((String) null, "", "EventTypeQueNoExisteTodavia", "CatalogUpdated");
    }

}

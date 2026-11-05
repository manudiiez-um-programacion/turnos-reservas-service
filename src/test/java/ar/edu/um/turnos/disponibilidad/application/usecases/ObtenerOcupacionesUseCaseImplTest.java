package ar.edu.um.turnos.disponibilidad.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.disponibilidad.application.exception.RangoFechasInvalidoException;
import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.ports.out.ConsultarOcupacionesCatedraPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObtenerOcupacionesUseCaseImpl")
class ObtenerOcupacionesUseCaseImplTest {

    private static final Long PROFESIONAL_ID = 15L;
    private static final LocalDate FROM = LocalDate.of(2026, 9, 14);
    private static final LocalDate TO = LocalDate.of(2026, 9, 14);

    @Mock
    private ConsultarOcupacionesCatedraPort consultarOcupacionesCatedraPort;

    private ObtenerOcupacionesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ObtenerOcupacionesUseCaseImpl(consultarOcupacionesCatedraPort);
    }

    @Test
    @DisplayName("rechaza un 'from' posterior a 'to' sin consultar a la catedra")
    void rechazaUnFromPosteriorAToSinConsultar() {
        LocalDate from = LocalDate.of(2026, 9, 15);
        LocalDate to = LocalDate.of(2026, 9, 14);

        assertThatThrownBy(() -> useCase.obtener(PROFESIONAL_ID, from, to))
                .isInstanceOf(RangoFechasInvalidoException.class);

        verify(consultarOcupacionesCatedraPort, never()).consultar(PROFESIONAL_ID, from, to);
    }

    @Test
    @DisplayName("devuelve las ocupaciones de slots distintos sin tocarlas")
    void devuelveOcupacionesDeSlotsDistintosSinTocarlas() {
        Ocupacion confirmada = ocupacion(LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null);
        Ocupacion retenida = ocupacion(LocalTime.of(9, 30), EstadoOcupacion.RETENIDA, Instant.parse("2026-09-10T14:44:50Z"));
        when(consultarOcupacionesCatedraPort.consultar(PROFESIONAL_ID, FROM, TO)).thenReturn(List.of(confirmada, retenida));

        List<Ocupacion> resultado = useCase.obtener(PROFESIONAL_ID, FROM, TO);

        assertThat(resultado).containsExactly(confirmada, retenida);
    }

    @Test
    @DisplayName("una lista vacia de ocupaciones es una respuesta valida")
    void unaListaVaciaEsValida() {
        when(consultarOcupacionesCatedraPort.consultar(PROFESIONAL_ID, FROM, TO)).thenReturn(List.of());

        assertThat(useCase.obtener(PROFESIONAL_ID, FROM, TO)).isEmpty();
    }

    @Test
    @DisplayName("si el mismo slot aparece como CONFIRMADA y RETENIDA, prevalece CONFIRMADA")
    void siElMismoSlotApareceDosVecesPrevaleceConfirmada() {
        LocalTime horaInicio = LocalTime.of(9, 0);
        Ocupacion retenida = ocupacion(horaInicio, EstadoOcupacion.RETENIDA, Instant.parse("2026-09-10T14:44:50Z"));
        Ocupacion confirmada = ocupacion(horaInicio, EstadoOcupacion.CONFIRMADA, null);
        when(consultarOcupacionesCatedraPort.consultar(PROFESIONAL_ID, FROM, TO)).thenReturn(List.of(retenida, confirmada));

        List<Ocupacion> resultado = useCase.obtener(PROFESIONAL_ID, FROM, TO);

        assertThat(resultado).containsExactly(confirmada);
    }

    @Test
    @DisplayName("el orden de llegada no cambia cual prevalece para el mismo slot")
    void elOrdenDeLlegadaNoCambiaCualPrevalece() {
        LocalTime horaInicio = LocalTime.of(9, 0);
        Ocupacion confirmada = ocupacion(horaInicio, EstadoOcupacion.CONFIRMADA, null);
        Ocupacion retenida = ocupacion(horaInicio, EstadoOcupacion.RETENIDA, Instant.parse("2026-09-10T14:44:50Z"));
        when(consultarOcupacionesCatedraPort.consultar(PROFESIONAL_ID, FROM, TO)).thenReturn(List.of(confirmada, retenida));

        List<Ocupacion> resultado = useCase.obtener(PROFESIONAL_ID, FROM, TO);

        assertThat(resultado).containsExactly(confirmada);
    }

    private Ocupacion ocupacion(LocalTime horaInicio, EstadoOcupacion estado, Instant expiraEn) {
        return new Ocupacion(FROM, horaInicio, estado, expiraEn);
    }

}

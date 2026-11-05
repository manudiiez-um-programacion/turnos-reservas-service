package ar.edu.um.turnos.disponibilidad.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.OcupacionesResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.disponibilidad.application.exception.RangoFechasInvalidoException;
import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper.OcupacionCatedraMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarOcupacionesCatedraAdapter")
class ConsultarOcupacionesCatedraAdapterTest {

    private static final Long PROFESIONAL_ID = 15L;
    private static final LocalDate FROM = LocalDate.of(2026, 9, 14);
    private static final LocalDate TO = LocalDate.of(2026, 9, 14);

    @Mock
    private CatedraClient catedraClient;

    private final OcupacionCatedraMapper ocupacionCatedraMapper = new OcupacionCatedraMapper();

    private ConsultarOcupacionesCatedraAdapter adapter;

    @Test
    @DisplayName("delega en CatedraClient y traduce la respuesta al modelo de dominio")
    void delegaYTraduceLaRespuesta() {
        adapter = new ConsultarOcupacionesCatedraAdapter(catedraClient, ocupacionCatedraMapper);
        var ocupacionCatedra = new ar.edu.um.turnos.config.catedra.dto.Ocupacion(
                FROM, LocalTime.of(9, 0), "CONFIRMED", null);
        when(catedraClient.obtenerOcupaciones(PROFESIONAL_ID, FROM, TO))
                .thenReturn(new OcupacionesResponse(PROFESIONAL_ID, FROM, TO, List.of(ocupacionCatedra)));

        List<Ocupacion> resultado = adapter.consultar(PROFESIONAL_ID, FROM, TO);

        assertThat(resultado).containsExactly(new Ocupacion(FROM, LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null));
    }

    @Test
    @DisplayName("traduce INVALID_DATE_RANGE a RangoFechasInvalidoException")
    void traduceInvalidDateRangeARangoFechasInvalidoException() {
        adapter = new ConsultarOcupacionesCatedraAdapter(catedraClient, ocupacionCatedraMapper);
        when(catedraClient.obtenerOcupaciones(PROFESIONAL_ID, FROM, TO))
                .thenThrow(new CatedraClientErrorException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.consultar(PROFESIONAL_ID, FROM, TO))
                .isInstanceOf(RangoFechasInvalidoException.class)
                .hasCauseInstanceOf(CatedraClientErrorException.class);
    }

    @Test
    @DisplayName("no traduce otros errores funcionales: los deja propagar sin tocar")
    void noTraduceOtrosErroresFuncionales() {
        adapter = new ConsultarOcupacionesCatedraAdapter(catedraClient, ocupacionCatedraMapper);
        CatedraClientErrorException original =
                new CatedraClientErrorException(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "detalle", List.of());
        when(catedraClient.obtenerOcupaciones(PROFESIONAL_ID, FROM, TO)).thenThrow(original);

        assertThatThrownBy(() -> adapter.consultar(PROFESIONAL_ID, FROM, TO)).isSameAs(original);
    }

    @Test
    @DisplayName("no traduce un error de infraestructura: lo deja propagar sin tocar")
    void noTraduceUnErrorDeInfraestructura() {
        adapter = new ConsultarOcupacionesCatedraAdapter(catedraClient, ocupacionCatedraMapper);
        CatedraServerErrorException original =
                new CatedraServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, "no disponible", List.of());
        when(catedraClient.obtenerOcupaciones(PROFESIONAL_ID, FROM, TO)).thenThrow(original);

        assertThatThrownBy(() -> adapter.consultar(PROFESIONAL_ID, FROM, TO)).isSameAs(original);
    }

}

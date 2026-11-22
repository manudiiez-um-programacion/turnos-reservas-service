package ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;

@DisplayName("OcupacionCatedraMapper")
class OcupacionCatedraMapperTest {

    private final OcupacionCatedraMapper mapper = new OcupacionCatedraMapper();

    @Test
    @DisplayName("traduce CONFIRMED y HELD al estado de dominio correspondiente")
    void traduceConfirmedYHeldAlEstadoDeDominio() {
        var confirmada = new ar.edu.um.turnos.config.catedra.dto.Ocupacion(
                LocalDate.of(2026, 9, 14), LocalTime.of(9, 0), "CONFIRMED", null);
        var retenida = new ar.edu.um.turnos.config.catedra.dto.Ocupacion(
                LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), "HELD", Instant.parse("2026-09-10T14:44:50Z"));

        List<Ocupacion> resultado = mapper.aDominio(List.of(confirmada, retenida));

        assertThat(resultado).containsExactly(
                new Ocupacion(LocalDate.of(2026, 9, 14), LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null),
                new Ocupacion(LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), EstadoOcupacion.RETENIDA,
                        Instant.parse("2026-09-10T14:44:50Z")));
    }

    @Test
    @DisplayName("un status desconocido se traduce a DESCONOCIDO sin romper el mapeo")
    void unStatusDesconocidoSeTraduceSinRomperElMapeo() {
        var ocupacion = new ar.edu.um.turnos.config.catedra.dto.Ocupacion(
                LocalDate.of(2026, 9, 14), LocalTime.of(9, 0), "OTRO", null);

        List<Ocupacion> resultado = mapper.aDominio(List.of(ocupacion));

        assertThat(resultado).singleElement().extracting(Ocupacion::estado).isEqualTo(EstadoOcupacion.DESCONOCIDO);
    }

    @Test
    @DisplayName("una lista nula se traduce a una lista vacia")
    void unaListaNulaSeTraduceAUnaListaVacia() {
        assertThat(mapper.aDominio(null)).isEmpty();
    }

}

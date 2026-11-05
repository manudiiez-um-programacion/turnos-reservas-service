package ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.config.catalogo.dto.HorarioSemanalVigente;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;

@DisplayName("ReglaHorariaSemanalCatalogoMapper")
class ReglaHorariaSemanalCatalogoMapperTest {

    private final ReglaHorariaSemanalCatalogoMapper mapper = new ReglaHorariaSemanalCatalogoMapper();

    @Test
    @DisplayName("traduce el dayOfWeek y marca la regla como habilitada")
    void traduceElDayOfWeekYMarcaLaReglaComoHabilitada() {
        HorarioSemanalVigente horario = new HorarioSemanalVigente(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);

        List<ReglaHorariaSemanal> resultado = mapper.aDominio(List.of(horario));

        assertThat(resultado).containsExactly(
                new ReglaHorariaSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
    }

    @Test
    @DisplayName("un dayOfWeek no reconocido se descarta sin romper el mapeo del resto")
    void unDayOfWeekNoReconocidoSeDescartaSinRomperElMapeo() {
        HorarioSemanalVigente invalido = new HorarioSemanalVigente(1L, "ALGUN_OTRO_VALOR", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        HorarioSemanalVigente valido = new HorarioSemanalVigente(2L, "TUESDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);

        List<ReglaHorariaSemanal> resultado = mapper.aDominio(List.of(invalido, valido));

        assertThat(resultado).containsExactly(
                new ReglaHorariaSemanal(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
    }

    @Test
    @DisplayName("una lista nula se traduce a una lista vacia")
    void unaListaNulaSeTraduceAUnaListaVacia() {
        assertThat(mapper.aDominio(null)).isEmpty();
    }

}

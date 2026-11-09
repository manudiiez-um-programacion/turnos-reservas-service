package ar.edu.um.turnos.disponibilidad.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import ar.edu.um.turnos.config.catalogo.CatalogoClient;
import ar.edu.um.turnos.config.catalogo.dto.AgendaProfesionalResponse;
import ar.edu.um.turnos.config.catalogo.dto.HorarioSemanalVigente;
import ar.edu.um.turnos.config.catalogo.dto.ProfesionalVigente;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoClientErrorException;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper.ReglaHorariaSemanalCatalogoMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarAgendaVigenteCatalogoAdapter")
class ConsultarAgendaVigenteCatalogoAdapterTest {

    private static final Long PROFESIONAL_ID = 15L;

    @Mock
    private CatalogoClient catalogoClient;

    private final ReglaHorariaSemanalCatalogoMapper reglaHorariaSemanalCatalogoMapper = new ReglaHorariaSemanalCatalogoMapper();

    private ConsultarAgendaVigenteCatalogoAdapter adapter;

    @Test
    @DisplayName("delega en CatalogoClient y traduce los horarios al modelo de dominio")
    void delegaYTraduceLosHorarios() {
        adapter = new ConsultarAgendaVigenteCatalogoAdapter(catalogoClient, reglaHorariaSemanalCatalogoMapper);
        HorarioSemanalVigente horario = new HorarioSemanalVigente(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        ProfesionalVigente profesional = new ProfesionalVigente(PROFESIONAL_ID, 1L, "Ana", "Gomez", true);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID))
                .thenReturn(new AgendaProfesionalResponse(profesional, List.of(horario), null));

        List<ReglaHorariaSemanal> resultado = adapter.consultar(PROFESIONAL_ID);

        assertThat(resultado).containsExactly(
                new ReglaHorariaSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
    }

    @Test
    @DisplayName("traduce PROFESSIONAL_NOT_FOUND a ProfesionalNoEncontradoException")
    void traducePROFESSIONAL_NOT_FOUND() {
        adapter = new ConsultarAgendaVigenteCatalogoAdapter(catalogoClient, reglaHorariaSemanalCatalogoMapper);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID))
                .thenThrow(new CatalogoClientErrorException(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "detalle"));

        assertThatThrownBy(() -> adapter.consultar(PROFESIONAL_ID))
                .isInstanceOf(ProfesionalNoEncontradoException.class)
                .hasCauseInstanceOf(CatalogoClientErrorException.class);
    }

    @Test
    @DisplayName("traduce PROFESSIONAL_DISABLED a ProfesionalDeshabilitadoException")
    void traducePROFESSIONAL_DISABLED() {
        adapter = new ConsultarAgendaVigenteCatalogoAdapter(catalogoClient, reglaHorariaSemanalCatalogoMapper);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID))
                .thenThrow(new CatalogoClientErrorException(HttpStatus.CONFLICT, "PROFESSIONAL_DISABLED", "detalle"));

        assertThatThrownBy(() -> adapter.consultar(PROFESIONAL_ID))
                .isInstanceOf(ProfesionalDeshabilitadoException.class)
                .hasCauseInstanceOf(CatalogoClientErrorException.class);
    }

    @Test
    @DisplayName("no traduce otros errores funcionales: los deja propagar sin tocar")
    void noTraduceOtrosErroresFuncionales() {
        adapter = new ConsultarAgendaVigenteCatalogoAdapter(catalogoClient, reglaHorariaSemanalCatalogoMapper);
        CatalogoClientErrorException original =
                new CatalogoClientErrorException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "detalle");
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID)).thenThrow(original);

        assertThatThrownBy(() -> adapter.consultar(PROFESIONAL_ID)).isSameAs(original);
    }

}

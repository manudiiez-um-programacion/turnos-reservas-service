package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.domain.model.ProfesionalParaHold;

@ExtendWith(MockitoExtension.class)
class ValidarSlotCatalogoAdapterTest {

    private static final Long PROFESIONAL_ID = 15L;
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 14); // lunes

    @Mock
    private CatalogoClient catalogoClient;

    private ValidarSlotCatalogoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ValidarSlotCatalogoAdapter(catalogoClient);
    }

    private AgendaProfesionalResponse agendaCon(HorarioSemanalVigente... horarios) {
        ProfesionalVigente profesional = new ProfesionalVigente(PROFESIONAL_ID, 1L, "Ana", "Gomez", true);
        return new AgendaProfesionalResponse(profesional, List.of(horarios), null);
    }

    @Test
    void devuelveElProfesionalCuandoElSlotEstaHabilitado() {
        HorarioSemanalVigente horario =
                new HorarioSemanalVigente(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID)).thenReturn(agendaCon(horario));

        ProfesionalParaHold resultado = adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 30));

        assertThat(resultado).isEqualTo(new ProfesionalParaHold("Ana", "Gomez"));
    }

    @Test
    void rechazaUnaHoraQueNoCoincideConNingunSlotDeLaRegla() {
        HorarioSemanalVigente horario =
                new HorarioSemanalVigente(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID)).thenReturn(agendaCon(horario));

        assertThatThrownBy(() -> adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 15)))
                .isInstanceOf(SlotInvalidoException.class);
    }

    @Test
    void rechazaUnaHoraQueExcedeElFinDelHorario() {
        HorarioSemanalVigente horario =
                new HorarioSemanalVigente(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID)).thenReturn(agendaCon(horario));

        assertThatThrownBy(() -> adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 45)))
                .isInstanceOf(SlotInvalidoException.class);
    }

    @Test
    void rechazaUnDiaDeLaSemanaSinReglas() {
        HorarioSemanalVigente horario =
                new HorarioSemanalVigente(1L, "TUESDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID)).thenReturn(agendaCon(horario));

        assertThatThrownBy(() -> adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 0)))
                .isInstanceOf(SlotInvalidoException.class);
    }

    @Test
    void traducePROFESSIONAL_NOT_FOUND() {
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID))
                .thenThrow(new CatalogoClientErrorException(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "detalle"));

        assertThatThrownBy(() -> adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 0)))
                .isInstanceOf(ProfesionalNoEncontradoException.class)
                .hasCauseInstanceOf(CatalogoClientErrorException.class);
    }

    @Test
    void traducePROFESSIONAL_DISABLED() {
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID))
                .thenThrow(new CatalogoClientErrorException(HttpStatus.CONFLICT, "PROFESSIONAL_DISABLED", "detalle"));

        assertThatThrownBy(() -> adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 0)))
                .isInstanceOf(ProfesionalDeshabilitadoException.class)
                .hasCauseInstanceOf(CatalogoClientErrorException.class);
    }

    @Test
    void noTraduceOtrosErroresFuncionales() {
        CatalogoClientErrorException original =
                new CatalogoClientErrorException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "detalle");
        when(catalogoClient.obtenerAgendaVigente(PROFESIONAL_ID)).thenThrow(original);

        assertThatThrownBy(() -> adapter.validar(PROFESIONAL_ID, FECHA, LocalTime.of(9, 0))).isSameAs(original);
    }

}

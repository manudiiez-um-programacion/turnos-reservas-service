package ar.edu.um.turnos.disponibilidad.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.GenerarSlotsUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerOcupacionesUseCase;
import ar.edu.um.turnos.disponibilidad.domain.ports.out.ConsultarAgendaVigenteCatalogoPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObtenerTurnosDisponiblesUseCaseImpl")
class ObtenerTurnosDisponiblesUseCaseImplTest {

    private static final Long PROFESIONAL_ID = 15L;
    // 2026-09-14 es lunes.
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 14);

    @Mock
    private ConsultarAgendaVigenteCatalogoPort consultarAgendaVigenteCatalogoPort;

    @Mock
    private ObtenerOcupacionesUseCase obtenerOcupacionesUseCase;

    @Mock
    private GenerarSlotsUseCase generarSlotsUseCase;

    private ObtenerTurnosDisponiblesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ObtenerTurnosDisponiblesUseCaseImpl(
                consultarAgendaVigenteCatalogoPort, obtenerOcupacionesUseCase, generarSlotsUseCase);
    }

    @Test
    @DisplayName("resta las ocupaciones a los slots generados y devuelve solo los libres")
    void restaLasOcupacionesYDevuelveSoloLosLibres() {
        List<ReglaHorariaSemanal> reglas = List.of(
                new ReglaHorariaSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
        List<Slot> slotsGenerados = List.of(
                new Slot(FECHA, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new Slot(FECHA, LocalTime.of(9, 30), LocalTime.of(10, 0)));
        Ocupacion ocupacion = new Ocupacion(FECHA, LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null);
        when(consultarAgendaVigenteCatalogoPort.consultar(PROFESIONAL_ID)).thenReturn(reglas);
        when(generarSlotsUseCase.generar(reglas, FECHA)).thenReturn(slotsGenerados);
        when(obtenerOcupacionesUseCase.obtener(PROFESIONAL_ID, FECHA, FECHA)).thenReturn(List.of(ocupacion));

        List<Slot> resultado = useCase.obtener(PROFESIONAL_ID, FECHA);

        assertThat(resultado).containsExactly(new Slot(FECHA, LocalTime.of(9, 30), LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("sin ocupaciones, devuelve todos los slots generados")
    void sinOcupacionesDevuelveTodosLosSlots() {
        List<ReglaHorariaSemanal> reglas = List.of(
                new ReglaHorariaSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
        List<Slot> slotsGenerados = List.of(new Slot(FECHA, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        when(consultarAgendaVigenteCatalogoPort.consultar(PROFESIONAL_ID)).thenReturn(reglas);
        when(generarSlotsUseCase.generar(reglas, FECHA)).thenReturn(slotsGenerados);
        when(obtenerOcupacionesUseCase.obtener(PROFESIONAL_ID, FECHA, FECHA)).thenReturn(List.of());

        List<Slot> resultado = useCase.obtener(PROFESIONAL_ID, FECHA);

        assertThat(resultado).containsExactly(new Slot(FECHA, LocalTime.of(9, 0), LocalTime.of(9, 30)));
    }

    @Test
    @DisplayName("un dia sin slots no consulta las ocupaciones de la catedra")
    void unDiaSinSlotsNoConsultaLasOcupaciones() {
        when(consultarAgendaVigenteCatalogoPort.consultar(PROFESIONAL_ID)).thenReturn(List.of());
        when(generarSlotsUseCase.generar(List.of(), FECHA)).thenReturn(List.of());

        List<Slot> resultado = useCase.obtener(PROFESIONAL_ID, FECHA);

        assertThat(resultado).isEmpty();
        verify(obtenerOcupacionesUseCase, never()).obtener(PROFESIONAL_ID, FECHA, FECHA);
    }

}

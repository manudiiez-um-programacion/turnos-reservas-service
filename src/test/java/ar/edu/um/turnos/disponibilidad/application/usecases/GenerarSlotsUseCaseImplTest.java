package ar.edu.um.turnos.disponibilidad.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;

@DisplayName("GenerarSlotsUseCaseImpl")
class GenerarSlotsUseCaseImplTest {

    // 2026-09-14 es lunes.
    private static final LocalDate LUNES = LocalDate.of(2026, 9, 14);
    private static final LocalDate MARTES = LocalDate.of(2026, 9, 15);

    private final GenerarSlotsUseCaseImpl useCase = new GenerarSlotsUseCaseImpl();

    @Test
    @DisplayName("genera los slots de startTime a endTime en pasos de slotDurationMinutes")
    void generaLosSlotsEnPasosDeLaDuracion() {
        ReglaHorariaSemanal regla = regla(DayOfWeek.MONDAY, "09:00", "10:00", 30, true);

        List<Slot> slots = useCase.generar(List.of(regla), LUNES);

        assertThat(slots).containsExactly(
                slot(LUNES, "09:00", "09:30"),
                slot(LUNES, "09:30", "10:00"));
    }

    @Test
    @DisplayName("un resto que no completa un slot no genera un slot parcial")
    void unRestoQueNoCompletaUnSlotNoGeneraUnSlotParcial() {
        ReglaHorariaSemanal regla = regla(DayOfWeek.MONDAY, "09:00", "10:10", 30, true);

        List<Slot> slots = useCase.generar(List.of(regla), LUNES);

        assertThat(slots).containsExactly(
                slot(LUNES, "09:00", "09:30"),
                slot(LUNES, "09:30", "10:00"));
    }

    @Test
    @DisplayName("solo expande las reglas cuyo dayOfWeek corresponde a la fecha")
    void soloExpandeLasReglasDelDiaCorrespondiente() {
        ReglaHorariaSemanal reglaLunes = regla(DayOfWeek.MONDAY, "09:00", "10:00", 30, true);
        ReglaHorariaSemanal reglaMartes = regla(DayOfWeek.TUESDAY, "09:00", "10:00", 30, true);

        List<Slot> slots = useCase.generar(List.of(reglaLunes, reglaMartes), LUNES);

        assertThat(slots).containsExactly(slot(LUNES, "09:00", "09:30"), slot(LUNES, "09:30", "10:00"));
    }

    @Test
    @DisplayName("no expande una regla deshabilitada")
    void noExpandeUnaReglaDeshabilitada() {
        ReglaHorariaSemanal reglaDeshabilitada = regla(DayOfWeek.MONDAY, "09:00", "10:00", 30, false);

        List<Slot> slots = useCase.generar(List.of(reglaDeshabilitada), LUNES);

        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("varias reglas del mismo dia se expanden todas")
    void variasReglasDelMismoDiaSeExpandenTodas() {
        ReglaHorariaSemanal reglaManana = regla(DayOfWeek.MONDAY, "09:00", "10:00", 30, true);
        ReglaHorariaSemanal reglaTarde = regla(DayOfWeek.MONDAY, "14:00", "15:00", 30, true);

        List<Slot> slots = useCase.generar(List.of(reglaManana, reglaTarde), LUNES);

        assertThat(slots).containsExactly(
                slot(LUNES, "09:00", "09:30"),
                slot(LUNES, "09:30", "10:00"),
                slot(LUNES, "14:00", "14:30"),
                slot(LUNES, "14:30", "15:00"));
    }

    @Test
    @DisplayName("si dos reglas se solapan, el resultado no duplica slots")
    void siDosReglasSeSolapanElResultadoNoDuplicaSlots() {
        ReglaHorariaSemanal reglaA = regla(DayOfWeek.MONDAY, "09:00", "10:00", 30, true);
        ReglaHorariaSemanal reglaB = regla(DayOfWeek.MONDAY, "09:30", "10:30", 30, true);

        List<Slot> slots = useCase.generar(List.of(reglaA, reglaB), LUNES);

        assertThat(slots).containsExactly(
                slot(LUNES, "09:00", "09:30"),
                slot(LUNES, "09:30", "10:00"),
                slot(LUNES, "10:00", "10:30"));
    }

    @Test
    @DisplayName("una lista de reglas vacia (profesional deshabilitado) devuelve una lista vacia")
    void unaListaDeReglasVaciaDevuelveUnaListaVacia() {
        assertThat(useCase.generar(List.of(), LUNES)).isEmpty();
    }

    @Test
    @DisplayName("un dia sin reglas que coincidan devuelve una lista vacia")
    void unDiaSinReglasQueCoincidanDevuelveUnaListaVacia() {
        ReglaHorariaSemanal reglaMartes = regla(DayOfWeek.TUESDAY, "09:00", "10:00", 30, true);

        assertThat(useCase.generar(List.of(reglaMartes), LUNES)).isEmpty();
    }

    @Test
    @DisplayName("acepta los siete dias de la semana del contrato")
    void aceptaLosSieteDiasDeLaSemana() {
        ReglaHorariaSemanal reglaMartes = regla(DayOfWeek.TUESDAY, "09:00", "09:30", 30, true);

        List<Slot> slots = useCase.generar(List.of(reglaMartes), MARTES);

        assertThat(slots).containsExactly(slot(MARTES, "09:00", "09:30"));
    }

    private ReglaHorariaSemanal regla(DayOfWeek dayOfWeek, String startTime, String endTime, int slotDurationMinutes,
            boolean enabled) {
        return new ReglaHorariaSemanal(dayOfWeek, LocalTime.parse(startTime), LocalTime.parse(endTime),
                slotDurationMinutes, enabled);
    }

    private Slot slot(LocalDate fecha, String horaInicio, String horaFin) {
        return new Slot(fecha, LocalTime.parse(horaInicio), LocalTime.parse(horaFin));
    }

}

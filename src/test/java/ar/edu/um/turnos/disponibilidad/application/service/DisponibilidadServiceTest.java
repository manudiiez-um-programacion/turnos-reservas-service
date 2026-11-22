package ar.edu.um.turnos.disponibilidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerTurnosDisponiblesUseCase;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock
    private ObtenerOcupacionesUseCase obtenerOcupacionesUseCase;

    @Mock
    private GenerarSlotsUseCase generarSlotsUseCase;

    @Mock
    private ObtenerTurnosDisponiblesUseCase obtenerTurnosDisponiblesUseCase;

    private DisponibilidadService service;

    @Test
    void delegaEnElCasoDeUsoDeOcupaciones() {
        service = new DisponibilidadService(obtenerOcupacionesUseCase, generarSlotsUseCase, obtenerTurnosDisponiblesUseCase);
        Long professionalId = 15L;
        LocalDate from = LocalDate.of(2026, 9, 14);
        LocalDate to = LocalDate.of(2026, 9, 14);
        List<Ocupacion> ocupaciones = List.of(new Ocupacion(from, LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null));
        when(obtenerOcupacionesUseCase.obtener(professionalId, from, to)).thenReturn(ocupaciones);

        List<Ocupacion> resultado = service.obtenerOcupaciones(professionalId, from, to);

        assertThat(resultado).isEqualTo(ocupaciones);
        verify(obtenerOcupacionesUseCase).obtener(professionalId, from, to);
    }

    @Test
    void delegaEnElCasoDeUsoDeGenerarSlots() {
        service = new DisponibilidadService(obtenerOcupacionesUseCase, generarSlotsUseCase, obtenerTurnosDisponiblesUseCase);
        LocalDate fecha = LocalDate.of(2026, 9, 14);
        List<ReglaHorariaSemanal> reglas = List.of(
                new ReglaHorariaSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
        List<Slot> slots = List.of(new Slot(fecha, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        when(generarSlotsUseCase.generar(reglas, fecha)).thenReturn(slots);

        List<Slot> resultado = service.generarSlots(reglas, fecha);

        assertThat(resultado).isEqualTo(slots);
        verify(generarSlotsUseCase).generar(reglas, fecha);
    }

    @Test
    void delegaEnElCasoDeUsoDeObtenerTurnosDisponibles() {
        service = new DisponibilidadService(obtenerOcupacionesUseCase, generarSlotsUseCase, obtenerTurnosDisponiblesUseCase);
        Long professionalId = 15L;
        LocalDate fecha = LocalDate.of(2026, 9, 14);
        List<Slot> slots = List.of(new Slot(fecha, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        when(obtenerTurnosDisponiblesUseCase.obtener(professionalId, fecha)).thenReturn(slots);

        List<Slot> resultado = service.obtenerTurnosDisponibles(professionalId, fecha);

        assertThat(resultado).isEqualTo(slots);
        verify(obtenerTurnosDisponiblesUseCase).obtener(professionalId, fecha);
    }

}

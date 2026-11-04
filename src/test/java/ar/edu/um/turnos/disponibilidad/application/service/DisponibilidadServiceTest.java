package ar.edu.um.turnos.disponibilidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.ports.in.ObtenerOcupacionesUseCase;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock
    private ObtenerOcupacionesUseCase obtenerOcupacionesUseCase;

    @Test
    void delegaEnElCasoDeUso() {
        DisponibilidadService service = new DisponibilidadService(obtenerOcupacionesUseCase);
        Long professionalId = 15L;
        LocalDate from = LocalDate.of(2026, 9, 14);
        LocalDate to = LocalDate.of(2026, 9, 14);
        List<Ocupacion> ocupaciones = List.of(new Ocupacion(from, LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null));
        when(obtenerOcupacionesUseCase.obtener(professionalId, from, to)).thenReturn(ocupaciones);

        List<Ocupacion> resultado = service.obtenerOcupaciones(professionalId, from, to);

        assertThat(resultado).isEqualTo(ocupaciones);
        verify(obtenerOcupacionesUseCase).obtener(professionalId, from, to);
    }

}

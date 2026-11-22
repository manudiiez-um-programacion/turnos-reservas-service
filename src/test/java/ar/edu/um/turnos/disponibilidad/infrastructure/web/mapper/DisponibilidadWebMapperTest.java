package ar.edu.um.turnos.disponibilidad.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.dto.DisponibilidadResponse;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.dto.SlotResponse;

class DisponibilidadWebMapperTest {

    private final DisponibilidadWebMapper mapper = new DisponibilidadWebMapper();

    @Test
    void traduceLosSlotsDeDominioAlaRespuesta() {
        LocalDate fecha = LocalDate.of(2026, 9, 14);
        List<Slot> slots = List.of(new Slot(fecha, LocalTime.of(9, 0), LocalTime.of(9, 30)));

        DisponibilidadResponse respuesta = mapper.toResponse(15L, fecha, slots);

        assertThat(respuesta).isEqualTo(new DisponibilidadResponse(15L, fecha,
                List.of(new SlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)))));
    }

}

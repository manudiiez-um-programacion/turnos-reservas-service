package ar.edu.um.turnos.disponibilidad.infrastructure.web.mapper;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.dto.DisponibilidadResponse;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.dto.SlotResponse;

@Component
public class DisponibilidadWebMapper {

    public DisponibilidadResponse toResponse(Long professionalId, LocalDate date, List<Slot> slots) {
        List<SlotResponse> disponibles = slots.stream()
                .map(slot -> new SlotResponse(slot.horaInicio(), slot.horaFin()))
                .toList();
        return new DisponibilidadResponse(professionalId, date, disponibles);
    }

}

package ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;

/**
 * Traduce la respuesta de {@code GET /api/appointment-occupancies} (el DTO de
 * {@code CatedraClient}) al modelo de dominio de este slice (#22).
 */
@Component
public class OcupacionCatedraMapper {

    public List<Ocupacion> aDominio(List<ar.edu.um.turnos.config.catedra.dto.Ocupacion> ocupaciones) {
        if (ocupaciones == null) {
            return List.of();
        }
        return ocupaciones.stream().map(this::aDominio).toList();
    }

    private Ocupacion aDominio(ar.edu.um.turnos.config.catedra.dto.Ocupacion ocupacion) {
        return new Ocupacion(
                ocupacion.date(),
                ocupacion.startTime(),
                EstadoOcupacion.desde(ocupacion.status()),
                ocupacion.expiresAt());
    }

}

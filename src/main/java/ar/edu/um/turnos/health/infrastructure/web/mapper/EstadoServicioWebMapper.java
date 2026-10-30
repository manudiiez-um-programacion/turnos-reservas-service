package ar.edu.um.turnos.health.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.health.domain.model.EstadoServicio;
import ar.edu.um.turnos.health.infrastructure.web.dto.EstadoServicioResponse;

/**
 * Traduce el modelo de dominio al DTO de la API.
 *
 * <p>Este mapeo en el borde es lo que permite cambiar el contrato HTTP sin tocar
 * el dominio, y viceversa. Hace guarda de nulos, como todos los mappers del
 * proyecto.
 */
@Component
public class EstadoServicioWebMapper {

    public EstadoServicioResponse toResponse(EstadoServicio estadoServicio) {
        if (estadoServicio == null) {
            return null;
        }
        return new EstadoServicioResponse(
                estadoServicio.getNombre(),
                estadoServicio.getVersion(),
                estadoServicio.getEstado() == null ? null : estadoServicio.getEstado().name(),
                estadoServicio.getConsultadoEn());
    }

}

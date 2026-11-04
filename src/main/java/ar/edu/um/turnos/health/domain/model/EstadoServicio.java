package ar.edu.um.turnos.health.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado operativo del servicio, tal como lo entiende el dominio.
 *
 * <p>Modelo de dominio puro: sin anotaciones de JPA, de validacion ni de Jackson.
 * Es lo que viaja entre {@code application} y {@code infrastructure}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoServicio {

    private String nombre;
    private String version;
    private Estado estado;
    private Instant consultadoEn;

    public enum Estado {
        UP,
        DOWN
    }

}

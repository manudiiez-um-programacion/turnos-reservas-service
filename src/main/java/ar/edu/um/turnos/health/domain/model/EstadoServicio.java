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
    private EstadoDependencias dependencias;

    /**
     * {@code DEGRADADO} es lo que agrega #40: la base de datos esta disponible
     * (el servicio puede atender las operaciones que dependen solo de datos
     * propios, §3) pero la catedra o el catalogo no, asi que las operaciones
     * que necesitan validar informacion vigente no pueden iniciarse. Distinto
     * de {@code DOWN}, que es exclusivo de la base caida: sin ella el
     * servicio no puede operar en absoluto.
     */
    public enum Estado {
        UP,
        DEGRADADO,
        DOWN
    }

}

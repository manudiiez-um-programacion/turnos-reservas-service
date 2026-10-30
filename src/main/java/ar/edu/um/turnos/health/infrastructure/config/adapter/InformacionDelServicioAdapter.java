package ar.edu.um.turnos.health.infrastructure.config.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.health.domain.ports.out.InformacionDelServicioPort;

/**
 * Adaptador del puerto de salida: resuelve los datos del servicio leyendo la
 * configuracion de Spring.
 *
 * <p>Este es el patron para cualquier dependencia externa del dominio. Cuando el
 * slice necesita base de datos, el equivalente vive en
 * {@code infrastructure/persistence/adapter/} y delega en un
 * {@code JpaRepository}; cuando necesita HTTP, en
 * {@code infrastructure/client/adapter/}.
 */
@Component
public class InformacionDelServicioAdapter implements InformacionDelServicioPort {

    private final String nombre;
    private final String version;

    public InformacionDelServicioAdapter(
            @Value("${spring.application.name}") String nombre,
            @Value("${turnos.version}") String version) {
        this.nombre = nombre;
        this.version = version;
    }

    @Override
    public String nombre() {
        return nombre;
    }

    @Override
    public String version() {
        return version;
    }

}

package ar.edu.um.turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del servicio de turnos y reservas.
 *
 * <p>El codigo se organiza en <strong>slices verticales</strong>: un paquete
 * autocontenido por feature de negocio, cada uno con sus tres capas hexagonales
 * ({@code domain}, {@code application}, {@code infrastructure}). El slice
 * {@code health} es la referencia de esa estructura.
 *
 * <p>{@code @ConfigurationPropertiesScan} registra las clases de configuracion
 * tipada del paquete {@code config} sin tener que enumerarlas en un
 * {@code @EnableConfigurationProperties}. Al ser {@code @Validated}, una
 * propiedad obligatoria que falte impide el arranque.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TurnosReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurnosReservasApplication.class, args);
    }

}

package ar.edu.um.turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del servicio de turnos y reservas.
 *
 * <p>El codigo se organiza en <strong>slices verticales</strong>: un paquete
 * autocontenido por feature de negocio, cada uno con sus tres capas hexagonales
 * ({@code domain}, {@code application}, {@code infrastructure}). El slice
 * {@code health} es la referencia de esa estructura.
 */
@SpringBootApplication
public class TurnosReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurnosReservasApplication.class, args);
    }

}

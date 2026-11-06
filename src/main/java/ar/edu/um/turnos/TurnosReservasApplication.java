package ar.edu.um.turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
 *
 * <p>{@code @EnableAsync} habilita {@code @Async} (#37): lo usa
 * {@code RecuperacionArranqueListener} para que la recuperacion de procesos
 * pendientes al arrancar no bloquee el hilo de arranque. Spring Boot ya
 * autoconfigura el {@code TaskExecutor} que {@code @Async} usa por defecto,
 * asi que no hace falta declarar uno propio.
 *
 * <p>{@code @EnableScheduling} habilita {@code @Scheduled} (#38): lo usa
 * {@code CierreVencidosScheduler} para la tarea periodica que cierra los
 * holds y procesos vencidos.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class TurnosReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurnosReservasApplication.class, args);
    }

}

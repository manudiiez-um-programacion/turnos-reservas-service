package ar.edu.um.turnos.reservas.infrastructure.lifecycle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.service.ReservasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que cierra los holds y procesos de reserva vencidos
 * (#38, §7 y §9 de la referencia). Solo delega en {@link ReservasService}:
 * la infraestructura no decide la logica de cierre, eso es responsabilidad
 * del caso de uso.
 *
 * <p>{@code fixedDelayString} y no {@code fixedRateString} ni {@code cron}
 * a proposito: con {@code fixedDelay}, Spring espera a que termine una
 * corrida antes de programar la siguiente, asi que la tarea nunca se
 * superpone consigo misma sin necesidad de un lock propio (un solo
 * proceso de esta aplicacion la corre).
 *
 * <p>Condicional a {@code turnos.vencimiento.auto-startup} (default
 * {@code true}, en {@code false} en el perfil de pruebas), mismo motivo que
 * {@link RecuperacionArranqueListener} y que el oyente de Kafka de #18: sin
 * el toggle, cualquier prueba con {@code @SpringBootTest} —no solo las de
 * este slice— terminaria disparando una consulta JPA real contra una base
 * que en ese perfil no esta corriendo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "turnos.vencimiento.auto-startup", havingValue = "true", matchIfMissing = true)
public class CierreVencidosScheduler {

    private final ReservasService reservasService;

    @Scheduled(fixedDelayString = "${turnos.vencimiento.frecuencia}")
    public void cerrarVencidos() {
        try {
            reservasService.cerrarProcesosVencidos();
        } catch (RuntimeException fallo) {
            log.error("El cierre programado de procesos vencidos termino con un error inesperado: {}",
                    fallo.getMessage(), fallo);
        }
    }

}

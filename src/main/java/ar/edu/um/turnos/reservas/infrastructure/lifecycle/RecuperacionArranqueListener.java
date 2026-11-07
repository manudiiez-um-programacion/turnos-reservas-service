package ar.edu.um.turnos.reservas.infrastructure.lifecycle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.service.ReservasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispara la recuperacion de procesos pendientes al terminar de arrancar el
 * servicio (#37, §4.2 y §8 del enunciado). Solo delega en
 * {@link ReservasService}: la infraestructura no decide la logica de
 * recuperacion, eso es responsabilidad del caso de uso.
 *
 * <p>{@code @Async} para que la recuperacion —que puede llamar a la
 * catedra— no bloquee el hilo de arranque ni demore que el servicio quede
 * disponible. El {@code try/catch} es una red adicional, mas alla de que
 * {@link ar.edu.um.turnos.reservas.application.usecases.RecuperarProcesosPendientesUseCaseImpl}
 * ya absorbe el fallo de la catedra por si mismo: evita el log generico de
 * Spring para una excepcion inesperada en un metodo {@code @Async}.
 *
 * <p>Condicional a {@code turnos.recuperacion.auto-startup} (default
 * {@code true}, en {@code false} en el perfil de pruebas): sin esto, cada
 * prueba que levanta el contexto completo con {@code @SpringBootTest}
 * —no solo las de este slice— dispararia una consulta JPA real al arrancar,
 * aunque el perfil de pruebas no tenga una base real con la que conectarse
 * (mismo motivo que apaga el oyente de Kafka en ese perfil, #18).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "turnos.recuperacion.auto-startup", havingValue = "true", matchIfMissing = true)
public class RecuperacionArranqueListener {

    private final ReservasService reservasService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        try {
            reservasService.recuperarProcesosPendientes();
        } catch (RuntimeException fallo) {
            log.error("La recuperacion de procesos pendientes al arrancar termino con un error inesperado: {}",
                    fallo.getMessage(), fallo);
        }
    }

}

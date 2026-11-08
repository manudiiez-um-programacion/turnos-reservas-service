package ar.edu.um.turnos.config.observabilidad;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Punto unico de los contadores y del ultimo estado conocido que exige el
 * issue #40 (§8, §11 y §19 de la referencia: reintentos, duplicados
 * descartados, vencidos cerrados y fallos de integracion observables).
 *
 * <p>Se inyecta directo en las clases que ya hacen la llamada real (los
 * clientes REST de la catedra y del catalogo, y los casos de uso que
 * descartan un evento duplicado o cierran un proceso vencido), igual que
 * {@code @Slf4j}: es un cross-cutting tecnico, no un puerto de dominio. Pedirle
 * a cada caso de uso un puerto de salida solo para incrementar un contador
 * seria envolver una libreria de metricas con una interfaz propia sin
 * ganar nada, el mismo motivo por el que el logging tampoco tiene puerto.
 *
 * <p>El MeterRegistry lo autoconfigura {@code spring-boot-starter-actuator}
 * (agregado solo por esto, ver pom.xml): sus endpoints HTTP quedan apagados
 * por {@code management.endpoints.enabled-by-default=false}.
 */
@Component
public class MetricasObservabilidad {

    private final MeterRegistry registry;
    private final Map<Integracion, AtomicReference<EstadoIntegracion>> estados = new EnumMap<>(Integracion.class);

    public MetricasObservabilidad(MeterRegistry registry) {
        this.registry = registry;
        for (Integracion integracion : Integracion.values()) {
            estados.put(integracion, new AtomicReference<>(EstadoIntegracion.desconocido()));
        }
    }

    /** Marca la integracion como disponible: la ultima llamada real tuvo exito. */
    public void exitoIntegracion(Integracion destino) {
        estados.get(destino).set(new EstadoIntegracion(true, Instant.now()));
    }

    /** Un intento se va a reintentar: no es todavia un fallo definitivo. */
    public void reintentoIntegracion(Integracion destino) {
        registry.counter("turnos.integracion.reintentos", "destino", nombre(destino)).increment();
    }

    /** Fallo definitivo de una llamada real (reintentos agotados, o una operacion sin reintento que no respondio). */
    public void falloIntegracion(Integracion destino) {
        registry.counter("turnos.integracion.fallos", "destino", nombre(destino)).increment();
        estados.get(destino).set(new EstadoIntegracion(false, Instant.now()));
    }

    /** Ultimo estado conocido de la integracion, sin generar una llamada nueva. */
    public EstadoIntegracion estadoDe(Integracion destino) {
        return estados.get(destino).get();
    }

    /** Un evento de Kafka con un {@code eventId} ya procesado, descartado sin repetir su efecto (§16, idempotencia). */
    public void eventoDuplicadoDescartado() {
        registry.counter("turnos.reservas.eventos.duplicados").increment();
    }

    /** Un hold o proceso que se cerro como VENCIDA por superar {@code expiresAt} (#38). */
    public void procesoVencidoCerrado() {
        registry.counter("turnos.reservas.procesos.vencidos").increment();
    }

    private String nombre(Integracion destino) {
        return destino.name().toLowerCase(Locale.ROOT);
    }

}

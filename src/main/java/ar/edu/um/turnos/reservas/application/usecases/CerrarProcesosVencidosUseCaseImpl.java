package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.VencimientoProperties;
import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.CerrarProcesosVencidosUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso (#38): revisa los procesos no finales
 * ({@link ProcesoReservaRepositoryPort#listarNoFinales()}) y cierra como
 * {@link EstadoProcesoReserva#VENCIDA} los que ya pasaron su {@code
 * expiresAt}, con el margen de {@link VencimientoProperties#margen()} para
 * no cerrar por una simple diferencia de reloj con la catedra.
 *
 * <p>Idempotente sin marca propia, igual que #37: solo mira procesos no
 * finales, asi que uno ya cerrado —por esta misma tarea en una corrida
 * anterior, por la confirmacion de #27/#28 al recibir {@code HOLD_EXPIRED},
 * o por el {@code AppointmentProcessExpired} de Kafka que procesa #31—
 * no vuelve a aparecer. Esa misma razon es la que hace que esta tarea
 * <strong>converja</strong> con un {@code AppointmentProcessExpired} tardio
 * en vez de pisarlo: {@link ProcesarEventoAccionUseCaseImpl} descarta ese
 * evento si el proceso ya esta en un estado final (como el que esta tarea
 * acaba de aplicar), y nunca retrocede un {@code VENCIDA} a otro estado.
 *
 * <p>No toca procesos vigentes ({@link EstadoProcesoReserva#HOLD_CREADO},
 * {@link EstadoProcesoReserva#CONFIRMACION_INICIAL_ENVIADA},
 * {@link EstadoProcesoReserva#ESPERANDO_TELEFONO}, {@link EstadoProcesoReserva#TELEFONO_ENVIADO},
 * {@link EstadoProcesoReserva#TELEFONO_RECHAZADO}) ni, por definicion de
 * {@code listarNoFinales}, los que ya estan en un estado final.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CerrarProcesosVencidosUseCaseImpl implements CerrarProcesosVencidosUseCase {

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;
    private final VencimientoProperties vencimientoProperties;
    private final MetricasObservabilidad metricas;

    @Override
    public int cerrarVencidos() {
        List<ProcesoReserva> pendientes = procesoReservaRepositoryPort.listarNoFinales();
        Instant limite = Instant.now().minus(vencimientoProperties.margen());

        int cerrados = 0;
        for (ProcesoReserva proceso : pendientes) {
            if (proceso.getExpiresAt() != null && proceso.getExpiresAt().isBefore(limite)) {
                cerrarComoVencido(proceso);
                cerrados++;
            }
        }

        if (cerrados > 0) {
            log.info("Cierre de vencidos: {} proceso(s) cerrado(s) como VENCIDA", cerrados);
        }
        return cerrados;
    }

    private void cerrarComoVencido(ProcesoReserva proceso) {
        ProcesoReserva cerrado = transicionarProcesoReservaUseCase.transicionar(proceso, EstadoProcesoReserva.VENCIDA, Instant.now());
        procesoReservaRepositoryPort.guardar(cerrado);
        metricas.procesoVencidoCerrado();
        log.info("Proceso {} vencido (expiraba {}): cerrado como VENCIDA",
                proceso.getReservationProcessId(), proceso.getExpiresAt());
    }

}

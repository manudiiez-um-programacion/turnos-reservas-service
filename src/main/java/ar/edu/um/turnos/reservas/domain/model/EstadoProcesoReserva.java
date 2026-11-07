package ar.edu.um.turnos.reservas.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados del proceso de reserva local (#26, §4.2 y §10 del enunciado: la
 * maquina de estados queda explicitamente a cargo del alumno).
 *
 * <p>Corresponde con el flujo descrito en §4 de la referencia, pero con mas
 * granularidad que los estados remotos que expone la catedra
 * ({@code PHONE_PENDING}/{@code CONFIRMED}/{@code CANCELLED}/{@code FAILED}
 * en {@code GET /appointments}): {@link #VENCIDA} e {@link #INVALIDA} son acá
 * dos estados distintos aunque la catedra los colapse a {@code FAILED} del
 * otro lado.
 *
 * <ul>
 *   <li>{@link #HOLD_CREADO}: tras el {@code 201} de crear el hold (#27).</li>
 *   <li>{@link #CONFIRMACION_INICIAL_ENVIADA}: se persiste al iniciar el
 *       {@code POST .../confirm} (#28), antes de conocer la respuesta, para
 *       poder reconciliar un timeout (#36) en lugar de reintentar a ciegas.</li>
 *   <li>{@link #ESPERANDO_TELEFONO}: {@code 202 WAITING_FOR_PHONE} recibido
 *       (o {@code RESERVATION_PROCESS_ALREADY_CONFIRMED} reconciliado).</li>
 *   <li>{@link #TELEFONO_ENVIADO}: se publico
 *       {@code AdditionalInformationSubmitted} (#30).</li>
 *   <li>{@link #TELEFONO_RECHAZADO}: llego {@code AdditionalInformationRejected};
 *       reintentable con un {@code eventId} nuevo antes de {@code expiresAt}.</li>
 *   <li>{@link #CONFIRMADA}, {@link #VENCIDA}, {@link #INVALIDA}: finales del
 *       proceso de reserva propiamente dicho.</li>
 *   <li>{@link #CANCELADA}: unica transicion permitida fuera de
 *       {@link #CONFIRMADA} (#34) — es una operacion nueva sobre una reserva ya
 *       concluida, no una reapertura del proceso.</li>
 * </ul>
 *
 * <p>{@link #CONFIRMADA}, igual que {@link #VENCIDA} e {@link #INVALIDA}, es
 * alcanzable desde cualquier estado no final y no solo desde
 * {@link #TELEFONO_ENVIADO} (#31): el orden entre REST y Kafka no esta
 * garantizado, y {@code AppointmentConfirmed} puede llegar antes de que se
 * haya persistido la respuesta del {@code confirm} o incluso del
 * {@code AdditionalInformationRequested}. La convergencia se resuelve asi,
 * aceptando el resultado que llegue primero sin duplicar la reserva.
 */
public enum EstadoProcesoReserva {

    HOLD_CREADO,
    CONFIRMACION_INICIAL_ENVIADA,
    ESPERANDO_TELEFONO,
    TELEFONO_ENVIADO,
    TELEFONO_RECHAZADO,
    CONFIRMADA,
    VENCIDA,
    INVALIDA,
    CANCELADA;

    /**
     * Estados que, dentro del proceso de reserva, no retroceden (§7 del
     * enunciado): un evento tardio no puede moverlos a otro estado del
     * proceso. {@link #CONFIRMADA} es final en ese sentido y a la vez el unico
     * que admite una transicion de negocio distinta ({@link #CANCELADA}, por
     * #34) — ver {@link #TRANSICIONES_VALIDAS}, que es la fuente de verdad
     * real sobre que se puede hacer desde cada estado.
     */
    private static final Set<EstadoProcesoReserva> ESTADOS_FINALES =
            EnumSet.of(CONFIRMADA, VENCIDA, INVALIDA, CANCELADA);

    private static final Map<EstadoProcesoReserva, Set<EstadoProcesoReserva>> TRANSICIONES_VALIDAS =
            construirTransicionesValidas();

    private static Map<EstadoProcesoReserva, Set<EstadoProcesoReserva>> construirTransicionesValidas() {
        Map<EstadoProcesoReserva, Set<EstadoProcesoReserva>> transiciones = new EnumMap<>(EstadoProcesoReserva.class);
        transiciones.put(HOLD_CREADO, EnumSet.of(CONFIRMACION_INICIAL_ENVIADA, CONFIRMADA, VENCIDA, INVALIDA));
        transiciones.put(CONFIRMACION_INICIAL_ENVIADA, EnumSet.of(ESPERANDO_TELEFONO, CONFIRMADA, VENCIDA, INVALIDA));
        transiciones.put(ESPERANDO_TELEFONO, EnumSet.of(TELEFONO_ENVIADO, CONFIRMADA, VENCIDA, INVALIDA));
        transiciones.put(TELEFONO_ENVIADO, EnumSet.of(CONFIRMADA, TELEFONO_RECHAZADO, VENCIDA, INVALIDA));
        transiciones.put(TELEFONO_RECHAZADO, EnumSet.of(TELEFONO_ENVIADO, CONFIRMADA, VENCIDA, INVALIDA));
        transiciones.put(CONFIRMADA, EnumSet.of(CANCELADA));
        transiciones.put(VENCIDA, EnumSet.noneOf(EstadoProcesoReserva.class));
        transiciones.put(INVALIDA, EnumSet.noneOf(EstadoProcesoReserva.class));
        transiciones.put(CANCELADA, EnumSet.noneOf(EstadoProcesoReserva.class));
        return transiciones;
    }

    public boolean esFinal() {
        return ESTADOS_FINALES.contains(this);
    }

    /**
     * Si se puede aplicar la transicion {@code this -> destino}. Una
     * transicion no listada (incluida cualquier salida de un estado sin
     * entradas en el mapa) se rechaza: no hay "else" implicito que la
     * permita silenciosamente.
     */
    public boolean puedeTransicionarA(EstadoProcesoReserva destino) {
        return TRANSICIONES_VALIDAS.getOrDefault(this, Set.of()).contains(destino);
    }

}

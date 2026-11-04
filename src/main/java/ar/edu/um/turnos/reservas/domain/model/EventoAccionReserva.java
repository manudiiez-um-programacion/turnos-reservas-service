package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Envoltorio comun a los eventos de {@code alumnos.turnos.acciones.{groupId}}
 * (§15.4-§15.10 de la referencia), con los campos que comparten las seis
 * variantes y que hacen falta para despachar y correlacionar (#18).
 *
 * <p>Los campos propios de cada variante (por ejemplo {@code reason},
 * {@code status} o {@code information.phoneNumber}) no estan modelados aca a
 * proposito: la logica de cada tipo de evento es responsabilidad de los casos
 * de uso que la implementan (#29, #31, #35), que son quienes necesitan leerlos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoAccionReserva {

    private String eventId;

    private TipoEventoAccion tipo;

    private String reservationProcessId;

    private String holdId;

    private Instant occurredAt;

    private int schemaVersion;

}

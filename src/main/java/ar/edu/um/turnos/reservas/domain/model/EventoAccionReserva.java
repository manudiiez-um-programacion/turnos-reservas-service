package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Envoltorio comun a los eventos de {@code alumnos.turnos.acciones.{groupId}}
 * (§15.4-§15.10 de la referencia), con los campos que comparten las seis
 * variantes y que hacen falta para despachar y correlacionar (#18), mas los
 * propios de {@code AdditionalInformationRequested} ({@code requiredField},
 * {@code message}, {@code expiresAt}) que necesita #29.
 *
 * <p>Los campos propios de las otras variantes (por ejemplo {@code reason},
 * {@code status} o {@code information.phoneNumber}) no estan modelados aca a
 * proposito: la logica de cada tipo de evento es responsabilidad de los casos
 * de uso que la implementan (#31, #35), que son quienes necesitan leerlos.
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

    /** {@code requiredField} de {@code AdditionalInformationRequested} (#29, §15.4). */
    private String requiredField;

    /** {@code message} de {@code AdditionalInformationRequested} (#29, §15.4). */
    private String message;

    /** {@code expiresAt} de {@code AdditionalInformationRequested}: nuevo plazo para completar el proceso (#29, §15.4). */
    private Instant expiresAt;

}

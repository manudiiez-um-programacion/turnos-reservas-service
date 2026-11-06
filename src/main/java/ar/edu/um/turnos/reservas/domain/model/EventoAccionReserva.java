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
 * <p>{@code reservationId} ({@code AppointmentConfirmed}) y {@code reason}
 * (comun a {@code AdditionalInformationRejected}, {@code
 * AppointmentProcessExpired} y {@code AppointmentProcessInvalid}) se agregan
 * para #31. {@code status} e {@code information.phoneNumber} siguen sin
 * modelarse aca: el primero es redundante con {@link #tipo}, y el segundo es
 * especifico de {@code AdditionalInformationSubmitted}, que es un evento de
 * salida y no de entrada (#35 todavia no los necesita).
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

    /** {@code expiresAt} de {@code AdditionalInformationRequested} o {@code AdditionalInformationRejected} (#29, #31). */
    private Instant expiresAt;

    /** {@code reservationId} de {@code AppointmentConfirmed} (#31, §15.6). */
    private Long reservationId;

    /**
     * {@code reason} de {@code AdditionalInformationRejected}, {@code
     * AppointmentProcessExpired} o {@code AppointmentProcessInvalid} (#31,
     * §15.7-§15.9). Valor crudo y no un enum: cada tipo de evento tiene su
     * propio vocabulario de motivos y no hace falta modelarlos para guardarlos
     * y mostrarlos.
     */
    private String reason;

}

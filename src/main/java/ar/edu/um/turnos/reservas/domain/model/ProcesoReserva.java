package ar.edu.um.turnos.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El proceso de reserva local (#26, §4.2 del enunciado): la unidad de trabajo
 * que correlaciona lo que pasa por REST con lo que llega por Kafka y
 * sobrevive a reinicios. No tiene anotaciones de framework a proposito: es
 * el objeto que circula por {@code application} y por los puertos, nunca la
 * {@code @Entity} de JPA.
 *
 * <p>{@code professionalNombreHistorico} y {@code professionalApellidoHistorico}
 * son el nombre y apellido del profesional segun el catalogo al momento de
 * crear el hold (#27): un dato descriptivo congelado de este proceso, nunca
 * una copia del catalogo vigente para validar operaciones nuevas (§4.2).
 *
 * <p>{@code expiresAt} es siempre el que informa la catedra: conservarlo
 * localmente no extiende el TTL (§9 de la referencia).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoReserva {

    private Long id;

    private String reservationProcessId;

    private String holdId;

    /** Usuario final dueño del proceso (§4.2): nunca se confia en un id que venga del cliente. */
    private Long usuarioId;

    private Long professionalId;

    private String professionalNombreHistorico;

    private String professionalApellidoHistorico;

    private LocalDate fecha;

    private LocalTime horaInicio;

    private EstadoProcesoReserva estado;

    private Instant expiresAt;

    /** {@code eventId} de {@code AdditionalInformationRequested}; se devuelve como {@code requestEventId} (#29). */
    private String requestEventId;

    /**
     * {@code requiredField} del ultimo {@code AdditionalInformationRequested}
     * recibido (#29). Hoy la catedra solo envia {@code PHONE_NUMBER}, pero se
     * guarda el valor crudo y no un enum: un valor distinto se registra en
     * vez de asumirse (§15.4 de la referencia).
     */
    private String requestRequiredField;

    /** {@code message} del ultimo {@code AdditionalInformationRequested} recibido (#29). */
    private String requestMessage;

    /** Telefono normalizado enviado a la catedra (#30, §15.5 de la referencia). Nunca se escribe en claro en los logs. */
    private String telefono;

    /** {@code reservationId} del lado de la catedra; solo existe una vez {@link EstadoProcesoReserva#CONFIRMADA}. */
    private Long reservationId;

    @Builder.Default
    private List<TransicionProcesoReserva> historial = new ArrayList<>();

}

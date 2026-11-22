package ar.edu.um.turnos.reservas.infrastructure.persistence.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Mapeo JPA de {@code evento_procesado} (V3__evento_procesado.sql, #20). No
 * sale de {@code infrastructure}: es un registro puramente tecnico para
 * deduplicar por {@code eventId}, el dominio no lo necesita como modelo.
 *
 * <p>{@code eventId} es el {@code @Id}: es la restriccion de la base la que
 * decide si un evento ya se proceso, nunca una consulta previa.
 */
@Entity
@Table(name = "evento_procesado")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcesadoJpaEntity {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(name = "reservation_process_id", nullable = false)
    private String reservationProcessId;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

}

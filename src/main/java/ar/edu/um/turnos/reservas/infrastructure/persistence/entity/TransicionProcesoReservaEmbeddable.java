package ar.edu.um.turnos.reservas.infrastructure.persistence.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Fila de {@code proceso_reserva_transicion} (V4__proceso_reserva.sql, #26):
 * un elemento del historial de {@link ProcesoReservaJpaEntity#getHistorial()}.
 * {@code @Embeddable} y no una entidad propia: es un valor sin identidad
 * propia, igual que las autoridades de {@code usuario_autoridad}.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TransicionProcesoReservaEmbeddable {

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

}

package ar.edu.um.turnos.reservas.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapeo JPA de {@code proceso_reserva} (V4__proceso_reserva.sql, #26). No
 * sale de {@code infrastructure}: el dominio trabaja con
 * {@link ar.edu.um.turnos.reservas.domain.model.ProcesoReserva}, nunca con
 * esto.
 */
@Entity
@Table(name = "proceso_reserva")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoReservaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_process_id", nullable = false, unique = true)
    private String reservationProcessId;

    @Column(name = "hold_id", nullable = false, unique = true)
    private String holdId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "professional_nombre_historico", nullable = false)
    private String professionalNombreHistorico;

    @Column(name = "professional_apellido_historico", nullable = false)
    private String professionalApellidoHistorico;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private String estado;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "request_event_id")
    private String requestEventId;

    @Column(name = "request_required_field")
    private String requestRequiredField;

    @Column(name = "request_message")
    private String requestMessage;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "proceso_reserva_transicion", joinColumns = @JoinColumn(name = "proceso_reserva_id"))
    @OrderBy("ocurridoEn ASC")
    private List<TransicionProcesoReservaEmbeddable> historial = new ArrayList<>();

}

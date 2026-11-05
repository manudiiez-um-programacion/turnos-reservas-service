package ar.edu.um.turnos.reservas.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.TransicionProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;

@DisplayName("ProcesoReservaPersistenceMapper")
class ProcesoReservaPersistenceMapperTest {

    private final ProcesoReservaPersistenceMapper mapper = new ProcesoReservaPersistenceMapper();

    private ProcesoReserva procesoDePrueba() {
        return ProcesoReserva.builder()
                .id(1L)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .usuarioId(42L)
                .professionalId(15L)
                .professionalNombreHistorico("Ana")
                .professionalApellidoHistorico("Gomez")
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .estado(EstadoProcesoReserva.ESPERANDO_TELEFONO)
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .requestEventId("request-event-8a31")
                .requestRequiredField("PHONE_NUMBER")
                .requestMessage("Ingrese un numero de telefono para continuar.")
                .reservationId(8251L)
                .historial(List.of(new TransicionProcesoReserva(
                        EstadoProcesoReserva.HOLD_CREADO, Instant.parse("2026-09-10T14:35:00Z"))))
                .build();
    }

    @Test
    @DisplayName("toEntity traduce todos los campos, incluido el historial")
    void toEntityTraduceTodosLosCampos() {
        ProcesoReservaJpaEntity entity = mapper.toEntity(procesoDePrueba());

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(entity.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(entity.getUsuarioId()).isEqualTo(42L);
        assertThat(entity.getProfessionalId()).isEqualTo(15L);
        assertThat(entity.getProfessionalNombreHistorico()).isEqualTo("Ana");
        assertThat(entity.getProfessionalApellidoHistorico()).isEqualTo("Gomez");
        assertThat(entity.getFecha()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(entity.getHoraInicio()).isEqualTo(LocalTime.of(9, 30));
        assertThat(entity.getEstado()).isEqualTo("ESPERANDO_TELEFONO");
        assertThat(entity.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
        assertThat(entity.getRequestEventId()).isEqualTo("request-event-8a31");
        assertThat(entity.getRequestRequiredField()).isEqualTo("PHONE_NUMBER");
        assertThat(entity.getRequestMessage()).isEqualTo("Ingrese un numero de telefono para continuar.");
        assertThat(entity.getReservationId()).isEqualTo(8251L);
        assertThat(entity.getHistorial()).hasSize(1);
        assertThat(entity.getHistorial().get(0).getEstado()).isEqualTo("HOLD_CREADO");
        assertThat(entity.getHistorial().get(0).getOcurridoEn()).isEqualTo(Instant.parse("2026-09-10T14:35:00Z"));
    }

    @Test
    @DisplayName("toDomain es el inverso de toEntity")
    void toDomainEsElInversoDeToEntity() {
        ProcesoReserva original = procesoDePrueba();

        ProcesoReserva resultado = mapper.toDomain(mapper.toEntity(original));

        assertThat(resultado.getId()).isEqualTo(original.getId());
        assertThat(resultado.getReservationProcessId()).isEqualTo(original.getReservationProcessId());
        assertThat(resultado.getHoldId()).isEqualTo(original.getHoldId());
        assertThat(resultado.getUsuarioId()).isEqualTo(original.getUsuarioId());
        assertThat(resultado.getProfessionalId()).isEqualTo(original.getProfessionalId());
        assertThat(resultado.getEstado()).isEqualTo(original.getEstado());
        assertThat(resultado.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(resultado.getRequestEventId()).isEqualTo(original.getRequestEventId());
        assertThat(resultado.getRequestRequiredField()).isEqualTo(original.getRequestRequiredField());
        assertThat(resultado.getRequestMessage()).isEqualTo(original.getRequestMessage());
        assertThat(resultado.getReservationId()).isEqualTo(original.getReservationId());
        assertThat(resultado.getHistorial()).isEqualTo(original.getHistorial());
    }

    @Test
    @DisplayName("toEntity y toDomain devuelven null para null")
    void devuelveNullParaNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }

}

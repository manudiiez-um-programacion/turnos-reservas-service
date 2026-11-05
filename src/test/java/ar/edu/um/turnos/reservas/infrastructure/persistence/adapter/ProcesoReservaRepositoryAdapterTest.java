package ar.edu.um.turnos.reservas.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;
import ar.edu.um.turnos.reservas.infrastructure.persistence.mapper.ProcesoReservaPersistenceMapper;
import ar.edu.um.turnos.reservas.infrastructure.persistence.repository.ProcesoReservaJpaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesoReservaRepositoryAdapter")
class ProcesoReservaRepositoryAdapterTest {

    @Mock
    private ProcesoReservaJpaRepository procesoReservaJpaRepository;

    private final ProcesoReservaPersistenceMapper mapper = new ProcesoReservaPersistenceMapper();

    private ProcesoReservaRepositoryAdapter adapter;

    private ProcesoReserva procesoDePrueba() {
        return ProcesoReserva.builder()
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .estado(EstadoProcesoReserva.HOLD_CREADO)
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .build();
    }

    @Test
    @DisplayName("guardar delega en el repositorio JPA a traves del mapper")
    void guardarDelegaEnElRepositorioJpa() {
        adapter = new ProcesoReservaRepositoryAdapter(procesoReservaJpaRepository, mapper);
        ProcesoReservaJpaEntity entityGuardada = mapper.toEntity(procesoDePrueba());
        when(procesoReservaJpaRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(entityGuardada);

        ProcesoReserva resultado = adapter.guardar(procesoDePrueba());

        assertThat(resultado.getReservationProcessId()).isEqualTo("process-a92b47d0");
    }

    @Test
    @DisplayName("buscarPorReservationProcessId devuelve vacio cuando no existe")
    void buscarPorReservationProcessIdDevuelveVacioCuandoNoExiste() {
        adapter = new ProcesoReservaRepositoryAdapter(procesoReservaJpaRepository, mapper);
        when(procesoReservaJpaRepository.findByReservationProcessId("process-inexistente"))
                .thenReturn(Optional.empty());

        Optional<ProcesoReserva> resultado = adapter.buscarPorReservationProcessId("process-inexistente");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("buscarPorHoldId traduce la entidad encontrada")
    void buscarPorHoldIdTraduceLaEntidadEncontrada() {
        adapter = new ProcesoReservaRepositoryAdapter(procesoReservaJpaRepository, mapper);
        when(procesoReservaJpaRepository.findByHoldId("hold-6c0c2f41"))
                .thenReturn(Optional.of(mapper.toEntity(procesoDePrueba())));

        Optional<ProcesoReserva> resultado = adapter.buscarPorHoldId("hold-6c0c2f41");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getHoldId()).isEqualTo("hold-6c0c2f41");
    }

}

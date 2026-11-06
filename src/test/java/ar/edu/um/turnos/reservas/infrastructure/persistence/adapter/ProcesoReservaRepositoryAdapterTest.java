package ar.edu.um.turnos.reservas.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
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

    @Test
    @DisplayName("buscarPorUsuario traduce la pagina de entidades encontradas")
    void buscarPorUsuarioTraduceLaPaginaEncontrada() {
        adapter = new ProcesoReservaRepositoryAdapter(procesoReservaJpaRepository, mapper);
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder().usuarioId(7L).pagina(0).tamanio(20).build();
        ProcesoReservaJpaEntity entity = mapper.toEntity(procesoDePrueba());
        Page<ProcesoReservaJpaEntity> pagina = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1L);
        when(procesoReservaJpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);

        PaginaProcesoReserva resultado = adapter.buscarPorUsuario(filtro);

        assertThat(resultado.totalElementos()).isEqualTo(1L);
        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).getReservationProcessId()).isEqualTo("process-a92b47d0");
    }

}

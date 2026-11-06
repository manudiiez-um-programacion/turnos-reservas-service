package ar.edu.um.turnos.reservas.infrastructure.persistence.adapter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;
import ar.edu.um.turnos.reservas.infrastructure.persistence.mapper.ProcesoReservaPersistenceMapper;
import ar.edu.um.turnos.reservas.infrastructure.persistence.repository.ProcesoReservaJpaRepository;
import ar.edu.um.turnos.reservas.infrastructure.persistence.specification.ProcesoReservaSpecifications;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcesoReservaRepositoryAdapter implements ProcesoReservaRepositoryPort {

    /** Mas reciente primero (#33): es el orden util para que KMP muestre el historial propio. */
    private static final Sort ORDEN = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("horaInicio"));

    private final ProcesoReservaJpaRepository procesoReservaJpaRepository;
    private final ProcesoReservaPersistenceMapper mapper;

    @Override
    public ProcesoReserva guardar(ProcesoReserva proceso) {
        return mapper.toDomain(procesoReservaJpaRepository.save(mapper.toEntity(proceso)));
    }

    @Override
    public Optional<ProcesoReserva> buscarPorReservationProcessId(String reservationProcessId) {
        return procesoReservaJpaRepository.findByReservationProcessId(reservationProcessId).map(mapper::toDomain);
    }

    @Override
    public Optional<ProcesoReserva> buscarPorHoldId(String holdId) {
        return procesoReservaJpaRepository.findByHoldId(holdId).map(mapper::toDomain);
    }

    @Override
    public PaginaProcesoReserva buscarPorUsuario(FiltroReservasUsuario filtro) {
        PageRequest pageable = PageRequest.of(filtro.pagina(), filtro.tamanio(), ORDEN);
        Page<ProcesoReservaJpaEntity> pagina =
                procesoReservaJpaRepository.findAll(ProcesoReservaSpecifications.deFiltro(filtro), pageable);
        return new PaginaProcesoReserva(pagina.map(mapper::toDomain).getContent(), pagina.getTotalElements());
    }

    @Override
    public Optional<ProcesoReserva> buscarVigentePorSlot(
            Long usuarioId, Long professionalId, LocalDate fecha, LocalTime horaInicio, Instant ahora) {
        return procesoReservaJpaRepository
                .findFirstByUsuarioIdAndProfessionalIdAndFechaAndHoraInicioAndEstadoNotInAndExpiresAtAfter(
                        usuarioId, professionalId, fecha, horaInicio, EstadoProcesoReserva.nombresFinales(), ahora)
                .map(mapper::toDomain);
    }

}

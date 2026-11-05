package ar.edu.um.turnos.reservas.infrastructure.persistence.adapter;

import java.util.Optional;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.infrastructure.persistence.mapper.ProcesoReservaPersistenceMapper;
import ar.edu.um.turnos.reservas.infrastructure.persistence.repository.ProcesoReservaJpaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcesoReservaRepositoryAdapter implements ProcesoReservaRepositoryPort {

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

}

package ar.edu.um.turnos.reservas.infrastructure.persistence.adapter;

import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.EventoProcesadoJpaEntity;
import ar.edu.um.turnos.reservas.infrastructure.persistence.repository.EventoProcesadoJpaRepository;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de {@link RegistrarEventoProcesadoPort} (#20): intenta insertar
 * la marca de "procesado" y deja que la restriccion de la base decida.
 *
 * <p>{@code saveAndFlush} y no {@code save}: el flush fuerza el INSERT
 * contra la base en este mismo metodo, asi que la violacion de la
 * restriccion (si otro hilo o instancia ya proceso este {@code eventId}) se
 * puede capturar aca en lugar de aparecer recien al cerrar la transaccion,
 * cuando ya seria tarde para decidir si ejecutar el efecto del evento.
 */
@Component
@RequiredArgsConstructor
public class EventoProcesadoRepositoryAdapter implements RegistrarEventoProcesadoPort {

    private final EventoProcesadoJpaRepository eventoProcesadoJpaRepository;

    @Override
    public boolean intentarRegistrar(EventoAccionReserva evento) {
        EventoProcesadoJpaEntity entity = EventoProcesadoJpaEntity.builder()
                .eventId(evento.getEventId())
                .tipoEvento(evento.getTipo().name())
                .reservationProcessId(evento.getReservationProcessId())
                .procesadoEn(Instant.now())
                .build();

        try {
            eventoProcesadoJpaRepository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException yaRegistrado) {
            return false;
        }
    }

}

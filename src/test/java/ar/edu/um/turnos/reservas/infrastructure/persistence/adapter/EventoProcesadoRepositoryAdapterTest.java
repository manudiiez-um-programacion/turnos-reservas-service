package ar.edu.um.turnos.reservas.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.EventoProcesadoJpaEntity;
import ar.edu.um.turnos.reservas.infrastructure.persistence.repository.EventoProcesadoJpaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventoProcesadoRepositoryAdapter")
class EventoProcesadoRepositoryAdapterTest {

    @Mock
    private EventoProcesadoJpaRepository eventoProcesadoJpaRepository;

    private EventoProcesadoRepositoryAdapter adapter;

    private EventoAccionReserva eventoDePrueba() {
        return EventoAccionReserva.builder()
                .eventId("confirmation-event-7d10")
                .tipo(TipoEventoAccion.RESERVA_CONFIRMADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .schemaVersion(1)
                .build();
    }

    @Test
    @DisplayName("registra el evento y devuelve true cuando es la primera vez que se ve el eventId")
    void registraYDevuelveTrueLaPrimeraVez() {
        adapter = new EventoProcesadoRepositoryAdapter(eventoProcesadoJpaRepository);

        boolean esNuevo = adapter.intentarRegistrar(eventoDePrueba());

        assertThat(esNuevo).isTrue();

        ArgumentCaptor<EventoProcesadoJpaEntity> captor = ArgumentCaptor.forClass(EventoProcesadoJpaEntity.class);
        verify(eventoProcesadoJpaRepository).saveAndFlush(captor.capture());

        EventoProcesadoJpaEntity guardado = captor.getValue();
        assertThat(guardado.getEventId()).isEqualTo("confirmation-event-7d10");
        assertThat(guardado.getTipoEvento()).isEqualTo("RESERVA_CONFIRMADA");
        assertThat(guardado.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(guardado.getProcesadoEn()).isNotNull();
    }

    @Test
    @DisplayName("devuelve false sin lanzar cuando el eventId ya estaba registrado")
    void devuelveFalseSinLanzarCuandoYaEstabaRegistrado() {
        adapter = new EventoProcesadoRepositoryAdapter(eventoProcesadoJpaRepository);
        when(eventoProcesadoJpaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("eventId duplicado"));

        boolean esNuevo = adapter.intentarRegistrar(eventoDePrueba());

        assertThat(esNuevo).isFalse();
    }

}

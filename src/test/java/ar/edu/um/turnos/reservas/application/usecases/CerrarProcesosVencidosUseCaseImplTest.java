package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import ar.edu.um.turnos.config.VencimientoProperties;
import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("CerrarProcesosVencidosUseCaseImpl")
class CerrarProcesosVencidosUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    private final TransicionarProcesoReservaUseCaseImpl transicionarProcesoReservaUseCase =
            new TransicionarProcesoReservaUseCaseImpl();

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MetricasObservabilidad metricas = new MetricasObservabilidad(registry);

    private CerrarProcesosVencidosUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        VencimientoProperties vencimientoProperties = new VencimientoProperties(Duration.ofSeconds(5), Duration.ofSeconds(30));
        useCase = new CerrarProcesosVencidosUseCaseImpl(
                procesoReservaRepositoryPort, transicionarProcesoReservaUseCase, vencimientoProperties, metricas);
    }

    private ProcesoReserva procesoDePrueba(String reservationProcessId, EstadoProcesoReserva estado, Instant expiresAt) {
        return ProcesoReserva.builder()
                .holdId("hold-" + reservationProcessId)
                .reservationProcessId(reservationProcessId)
                .usuarioId(7L)
                .externalPatientId("user-42")
                .professionalId(15L)
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .estado(estado)
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    @DisplayName("un proceso vencido se cierra como VENCIDA y un vigente no se toca")
    void soloElVencidoSeCierra() {
        ProcesoReserva vencido = procesoDePrueba(
                "process-vencido", EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now().minusSeconds(60));
        ProcesoReserva vigente = procesoDePrueba(
                "process-vigente", EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now().plusSeconds(600));
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of(vencido, vigente));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        int cerrados = useCase.cerrarVencidos();

        assertThat(cerrados).isEqualTo(1);
        assertThat(vencido.getEstado()).isEqualTo(EstadoProcesoReserva.VENCIDA);
        assertThat(vigente.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        verify(procesoReservaRepositoryPort).guardar(vencido);
        verify(procesoReservaRepositoryPort, never()).guardar(vigente);
        // #40: un proceso vencido cerrado, no dos: el vigente no cuenta.
        assertThat(registry.get("turnos.reservas.procesos.vencidos").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("un proceso vencido hace muy poco, dentro del margen, todavia no se cierra")
    void dentroDelMargenNoSeCierraTodavia() {
        ProcesoReserva reciente = procesoDePrueba(
                "process-reciente", EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now().minusSeconds(2));
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of(reciente));

        int cerrados = useCase.cerrarVencidos();

        assertThat(cerrados).isZero();
        assertThat(reciente.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("sin procesos no finales, no cierra nada")
    void sinProcesosNoFinalesNoCierraNada() {
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of());

        int cerrados = useCase.cerrarVencidos();

        assertThat(cerrados).isZero();
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

}

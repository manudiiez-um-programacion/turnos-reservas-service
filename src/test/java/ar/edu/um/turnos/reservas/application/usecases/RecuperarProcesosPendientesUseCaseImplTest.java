package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;
import ar.edu.um.turnos.reservas.domain.ports.in.CerrarProcesosVencidosUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ConsultarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;

/**
 * El cierre de vencidos en si (#38) se cubre en
 * {@code CerrarProcesosVencidosUseCaseImplTest}; aca se mockea
 * {@link CerrarProcesosVencidosUseCase} para probar solo la decision propia
 * de #37: que hacer con lo que queda vigente despues de ese cierre.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecuperarProcesosPendientesUseCaseImpl")
class RecuperarProcesosPendientesUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Mock
    private ConsultarReservaCatedraPort consultarReservaCatedraPort;

    @Mock
    private CerrarProcesosVencidosUseCase cerrarProcesosVencidosUseCase;

    private final TransicionarProcesoReservaUseCaseImpl transicionarProcesoReservaUseCase =
            new TransicionarProcesoReservaUseCaseImpl();

    private RecuperarProcesosPendientesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecuperarProcesosPendientesUseCaseImpl(
                procesoReservaRepositoryPort, consultarReservaCatedraPort,
                transicionarProcesoReservaUseCase, cerrarProcesosVencidosUseCase);
    }

    private ProcesoReserva procesoDePrueba(String reservationProcessId, EstadoProcesoReserva estado) {
        return ProcesoReserva.builder()
                .holdId("hold-" + reservationProcessId)
                .reservationProcessId(reservationProcessId)
                .usuarioId(7L)
                .externalPatientId("user-42")
                .professionalId(15L)
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .estado(estado)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
    }

    @Test
    @DisplayName("al arrancar, primero delega el cierre de vencidos en CerrarProcesosVencidosUseCase")
    void delegaElCierreDeVencidosAntesDeRevisarLoVigente() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(2);
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of());

        useCase.recuperar();

        verify(cerrarProcesosVencidosUseCase).cerrarVencidos();
    }

    @Test
    @DisplayName("HOLD_CREADO, ESPERANDO_TELEFONO, TELEFONO_ENVIADO y TELEFONO_RECHAZADO vigentes siguen esperando sin tocarse")
    void procesosVigentesEnEsperaNoSeTocan() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(0);
        List<ProcesoReserva> procesos = List.of(
                procesoDePrueba("process-hold", EstadoProcesoReserva.HOLD_CREADO),
                procesoDePrueba("process-espera", EstadoProcesoReserva.ESPERANDO_TELEFONO),
                procesoDePrueba("process-enviado", EstadoProcesoReserva.TELEFONO_ENVIADO),
                procesoDePrueba("process-rechazado", EstadoProcesoReserva.TELEFONO_RECHAZADO));
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(procesos);

        useCase.recuperar();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
        verify(consultarReservaCatedraPort, never()).buscarPorReservationProcessId(any(), any());
        assertThat(procesos).extracting(ProcesoReserva::getEstado).containsExactly(
                EstadoProcesoReserva.HOLD_CREADO, EstadoProcesoReserva.ESPERANDO_TELEFONO,
                EstadoProcesoReserva.TELEFONO_ENVIADO, EstadoProcesoReserva.TELEFONO_RECHAZADO);
    }

    @Test
    @DisplayName("CONFIRMACION_INICIAL_ENVIADA vigente: si la catedra ya la tiene, se alinea a ESPERANDO_TELEFONO")
    void confirmacionInicialEnviadaSeAlineaSiLaCatedraYaLaTiene() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(0);
        ProcesoReserva proceso = procesoDePrueba("process-1", EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-1"))
                .thenReturn(Optional.of(new ReservaCatedraEncontrada(null, "PHONE_PENDING", null, null)));

        useCase.recuperar();

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
    }

    @Test
    @DisplayName("CONFIRMACION_INICIAL_ENVIADA vigente: si la catedra todavia no la tiene, sigue esperando sin reintentar")
    void confirmacionInicialEnviadaSigueEsperandoSiLaCatedraNoLaTieneTodavia() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(0);
        ProcesoReserva proceso = procesoDePrueba("process-1", EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of(proceso));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-1"))
                .thenReturn(Optional.empty());

        useCase.recuperar();

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("CONFIRMACION_INICIAL_ENVIADA vigente: si la catedra no responde, no rompe la recuperacion")
    void confirmacionInicialEnviadaNoRompeLaRecuperacionSiLaCatedraNoResponde() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(0);
        ProcesoReserva proceso = procesoDePrueba("process-1", EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of(proceso));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-1"))
                .thenThrow(new CatedraServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, null, List.of()));

        useCase.recuperar();

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("con varios procesos vigentes, la recuperacion deja a cada uno en un estado coherente")
    void variosProcesosVigentesQuedanCoherentesTrasLaRecuperacion() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(1);
        ProcesoReserva holdVigente = procesoDePrueba("process-hold", EstadoProcesoReserva.HOLD_CREADO);
        ProcesoReserva confirmacionReconciliable =
                procesoDePrueba("process-reconciliable", EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        ProcesoReserva confirmacionSinNovedad =
                procesoDePrueba("process-sin-novedad", EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(
                List.of(holdVigente, confirmacionReconciliable, confirmacionSinNovedad));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-reconciliable"))
                .thenReturn(Optional.of(new ReservaCatedraEncontrada(null, "PHONE_PENDING", null, null)));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-sin-novedad"))
                .thenReturn(Optional.empty());

        useCase.recuperar();

        assertThat(holdVigente.getEstado()).isEqualTo(EstadoProcesoReserva.HOLD_CREADO);
        assertThat(confirmacionReconciliable.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        assertThat(confirmacionSinNovedad.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
    }

    @Test
    @DisplayName("sin procesos vigentes, la recuperacion no hace nada mas alla de delegar el cierre de vencidos")
    void sinProcesosVigentesNoHaceNadaMas() {
        when(cerrarProcesosVencidosUseCase.cerrarVencidos()).thenReturn(0);
        when(procesoReservaRepositoryPort.listarNoFinales()).thenReturn(List.of());

        useCase.recuperar();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
        verify(consultarReservaCatedraPort, never()).buscarPorReservationProcessId(any(), any());
    }

}

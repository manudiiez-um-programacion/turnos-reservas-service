package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.HoldYaConfirmadoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException;
import ar.edu.um.turnos.reservas.domain.model.ConfirmacionHoldCatedra;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;
import ar.edu.um.turnos.reservas.domain.ports.out.ConfirmarHoldCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ConsultarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmarHoldUseCaseImpl")
class ConfirmarHoldUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Mock
    private ConfirmarHoldCatedraPort confirmarHoldCatedraPort;

    @Mock
    private ConsultarReservaCatedraPort consultarReservaCatedraPort;

    private final TransicionarProcesoReservaUseCaseImpl transicionarProcesoReservaUseCase =
            new TransicionarProcesoReservaUseCaseImpl();

    private ConfirmarHoldUseCaseImpl useCase;

    private final SolicitudConfirmacionHold solicitud = SolicitudConfirmacionHold.builder()
            .holdId("hold-6c0c2f41")
            .usuarioId(7L)
            .externalPatientId("user-42")
            .patientFirstName("Juan")
            .patientLastName("Perez")
            .build();

    @BeforeEach
    void setUp() {
        useCase = new ConfirmarHoldUseCaseImpl(
                procesoReservaRepositoryPort, confirmarHoldCatedraPort, consultarReservaCatedraPort, transicionarProcesoReservaUseCase);
    }

    private ProcesoReserva procesoDePrueba(EstadoProcesoReserva estado) {
        return ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .usuarioId(7L)
                .professionalId(15L)
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .estado(estado)
                .build();
    }

    @Test
    @DisplayName("camino feliz: marca CONFIRMACION_INICIAL_ENVIADA, confirma y avanza a ESPERANDO_TELEFONO")
    void caminoFeliz() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenReturn(new ConfirmacionHoldCatedra("process-a92b47d0", "WAITING_FOR_PHONE", expiresAt));

        ProcesoReserva resultado = useCase.confirmar(solicitud);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        assertThat(resultado.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(resultado.getHistorial())
                .extracting(transicion -> transicion.estado())
                .containsExactly(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA, EstadoProcesoReserva.ESPERANDO_TELEFONO);
    }

    @Test
    @DisplayName("hold vencido: cierra el proceso local como VENCIDA y relanza la excepcion")
    void holdVencido() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new HoldVencidoException("hold-6c0c2f41", null));

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(HoldVencidoException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.VENCIDA);
    }

    @Test
    @DisplayName("hold no encontrado en la catedra: cierra el proceso local como INVALIDA y relanza la excepcion")
    void holdNoEncontradoEnLaCatedra() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new HoldNoEncontradoException("hold-6c0c2f41", null));

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(HoldNoEncontradoException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.INVALIDA);
    }

    @Test
    @DisplayName("RESERVATION_PROCESS_ALREADY_CONFIRMED se trata como exito idempotente")
    void confirmacionYaAplicadaPorLaCatedra() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new HoldYaConfirmadoException("hold-6c0c2f41", null));

        ProcesoReserva resultado = useCase.confirmar(solicitud);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
    }

    @Test
    @DisplayName("confirmacion repetida localmente: no vuelve a llamar a la catedra")
    void confirmacionRepetidaLocalmente() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        ProcesoReserva resultado = useCase.confirmar(solicitud);

        assertThat(resultado).isSameAs(proceso);
        verify(confirmarHoldCatedraPort, never()).confirmar(any(), any(), any(), any());
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("HOLD_OWNER_MISMATCH y EXTERNAL_PATIENT_MISMATCH se registran y se relanzan sin tocar el estado")
    void inconsistenciasSeRelanzanSinTocarElEstado() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new HoldOwnerMismatchException("hold-6c0c2f41", null));

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(HoldOwnerMismatchException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
    }

    @Test
    @DisplayName("EXTERNAL_PATIENT_MISMATCH se registra y se relanza sin tocar el estado")
    void externalPatientMismatchSeRelanza() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new ExternalPatientMismatchException("hold-6c0c2f41", null));

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(ExternalPatientMismatchException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
    }

    @Test
    @DisplayName("hold inexistente localmente: no llega a llamar a la catedra")
    void holdInexistenteLocalmente() {
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(HoldNoEncontradoException.class);

        verify(confirmarHoldCatedraPort, never()).confirmar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("solo el dueño del proceso puede confirmarlo")
    void soloElDuenioPuedeConfirmar() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        proceso.setUsuarioId(99L);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(ProcesoReservaAjenoException.class);

        verify(confirmarHoldCatedraPort, never()).confirmar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("un proceso en un estado final rechaza la confirmacion")
    void unProcesoEnEstadoFinalRechazaLaConfirmacion() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CANCELADA);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.confirmar(solicitud))
                .isInstanceOf(TransicionProcesoReservaInvalidaException.class);

        verify(confirmarHoldCatedraPort, never()).confirmar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("timeout confirmando: si la catedra ya tenia la reserva, se alinea sin reintentar")
    void timeoutConfirmandoLaCatedraYaLaTeniaAplicada() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new ResourceAccessException("timeout de lectura"));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-a92b47d0"))
                .thenReturn(Optional.of(new ReservaCatedraEncontrada(null, "PHONE_PENDING", null, null)));

        ProcesoReserva resultado = useCase.confirmar(solicitud);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        verify(confirmarHoldCatedraPort, times(1)).confirmar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("timeout confirmando + RESERVATION_PROCESS_ALREADY_CONFIRMED en el reintento: el proceso queda confirmado una sola vez")
    void timeoutConfirmandoYReintentoYaConfirmado() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new CatedraServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, null, List.of()))
                .thenThrow(new HoldYaConfirmadoException("hold-6c0c2f41", null));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-a92b47d0"))
                .thenReturn(Optional.empty());

        ProcesoReserva resultado = useCase.confirmar(solicitud);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        verify(confirmarHoldCatedraPort, times(2)).confirmar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("timeout confirmando y el reintento tambien da timeout: se propaga sin insistir mas")
    void timeoutConfirmandoYElReintentoTambienDaTimeout() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(confirmarHoldCatedraPort.confirmar(proceso, "user-42", "Juan", "Perez"))
                .thenThrow(new ResourceAccessException("timeout 1"))
                .thenThrow(new ResourceAccessException("timeout 2"));
        when(consultarReservaCatedraPort.buscarPorReservationProcessId("user-42", "process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.confirmar(solicitud)).isInstanceOf(ResourceAccessException.class);

        verify(confirmarHoldCatedraPort, times(2)).confirmar(any(), any(), any(), any());
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesarEventoAccionUseCaseImpl")
class ProcesarEventoAccionUseCaseImplTest {

    @Mock
    private RegistrarEventoProcesadoPort registrarEventoProcesadoPort;

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase =
            new TransicionarProcesoReservaUseCaseImpl();

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MetricasObservabilidad metricas = new MetricasObservabilidad(registry);

    private ProcesarEventoAccionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcesarEventoAccionUseCaseImpl(
                registrarEventoProcesadoPort, procesoReservaRepositoryPort, transicionarProcesoReservaUseCase, metricas);
    }

    private EventoAccionReserva.EventoAccionReservaBuilder eventoValido() {
        return EventoAccionReserva.builder()
                .eventId("confirmation-event-7d10")
                .tipo(TipoEventoAccion.RESERVA_CONFIRMADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .schemaVersion(1);
    }

    private ProcesoReserva.ProcesoReservaBuilder procesoDePrueba() {
        return ProcesoReserva.builder()
                .id(1L)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .usuarioId(42L)
                .estado(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
    }

    @Test
    @DisplayName("registra el evento como procesado y no lanza para un tipo conocido con schemaVersion 1")
    void registraYNoLanzaParaUnTipoConocidoConSchemaVersionSoportada() {
        EventoAccionReserva evento = eventoValido().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort).intentarRegistrar(evento);
    }

    @Test
    @DisplayName("descarta sin lanzar un eventId ya procesado, sin repetir su efecto")
    void descartaSinLanzarUnEventoYaProcesado() {
        EventoAccionReserva evento = eventoValido().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(false);

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort).intentarRegistrar(evento);
    }

    @Test
    @DisplayName("#40: un eventId ya procesado cuenta como evento duplicado descartado")
    void unEventoYaProcesadoCuentaComoDuplicadoDescartado() {
        EventoAccionReserva evento = eventoValido().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(false);

        useCase.procesar(evento);

        assertThat(registry.get("turnos.reservas.eventos.duplicados").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("descarta sin lanzar un eventType desconocido, sin consultar la deduplicacion")
    void descartaSinLanzarUnEventTypeDesconocido() {
        EventoAccionReserva evento = eventoValido().tipo(TipoEventoAccion.DESCONOCIDO).build();

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort, never()).intentarRegistrar(evento);
    }

    @Test
    @DisplayName("descarta sin lanzar una schemaVersion no soportada, sin consultar la deduplicacion")
    void descartaSinLanzarUnaSchemaVersionNoSoportada() {
        EventoAccionReserva evento = eventoValido().schemaVersion(2).build();

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort, never()).intentarRegistrar(evento);
    }

    private EventoAccionReserva.EventoAccionReservaBuilder solicitudInformacionAdicionalValida() {
        return EventoAccionReserva.builder()
                .eventId("request-event-8a31")
                .tipo(TipoEventoAccion.SOLICITUD_INFORMACION_ADICIONAL)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .requiredField("PHONE_NUMBER")
                .message("Ingrese un numero de telefono para continuar.")
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .schemaVersion(1);
    }

    @Test
    @DisplayName("AdditionalInformationRequested transiciona a ESPERANDO_TELEFONO cuando la confirmacion REST se perdio")
    void solicitudInformacionAdicionalTransicionaDesdeConfirmacionInicialEnviada() {
        EventoAccionReserva evento = solicitudInformacionAdicionalValida().build();
        ProcesoReserva proceso = procesoDePrueba().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        assertThat(guardado.getRequestEventId()).isEqualTo("request-event-8a31");
        assertThat(guardado.getRequestRequiredField()).isEqualTo("PHONE_NUMBER");
        assertThat(guardado.getRequestMessage()).isEqualTo("Ingrese un numero de telefono para continuar.");
        assertThat(guardado.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    @DisplayName("AdditionalInformationRequested no retransiciona si el proceso ya esta esperando el telefono, "
            + "pero actualiza el pedido mas reciente")
    void solicitudInformacionAdicionalActualizaSinRetransicionarSiYaEstabaEsperandoElTelefono() {
        EventoAccionReserva evento = solicitudInformacionAdicionalValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.ESPERANDO_TELEFONO).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        assertThat(guardado.getRequestEventId()).isEqualTo("request-event-8a31");
    }

    @Test
    @DisplayName("AdditionalInformationRequested se descarta sin lanzar cuando el reservationProcessId es desconocido")
    void solicitudInformacionAdicionalSeDescartaParaUnProcesoDesconocido() {
        EventoAccionReserva evento = solicitudInformacionAdicionalValida().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("AdditionalInformationRequested se descarta sin retroceder un proceso en estado final")
    void solicitudInformacionAdicionalSeDescartaParaUnProcesoEnEstadoFinal() {
        EventoAccionReserva evento = solicitudInformacionAdicionalValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.VENCIDA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        useCase.procesar(evento);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("un requiredField distinto de PHONE_NUMBER se registra igual, sin rechazar el evento")
    void solicitudInformacionAdicionalRegistraUnRequiredFieldDesconocido() {
        EventoAccionReserva evento = solicitudInformacionAdicionalValida().requiredField("OTRO_CAMPO").build();
        ProcesoReserva proceso = procesoDePrueba().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        assertThat(capturado.getValue().getRequestRequiredField()).isEqualTo("OTRO_CAMPO");
    }

    private EventoAccionReserva.EventoAccionReservaBuilder reservaConfirmadaValida() {
        return EventoAccionReserva.builder()
                .eventId("confirmation-event-7d10")
                .tipo(TipoEventoAccion.RESERVA_CONFIRMADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .reservationId(8251L)
                .schemaVersion(1);
    }

    @Test
    @DisplayName("AppointmentConfirmed guarda reservationId y transiciona a CONFIRMADA")
    void reservaConfirmadaGuardaReservationIdYTransicionaAConfirmada() {
        EventoAccionReserva evento = reservaConfirmadaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.TELEFONO_ENVIADO).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
        assertThat(guardado.getReservationId()).isEqualTo(8251L);
    }

    @Test
    @DisplayName("AppointmentConfirmed transiciona a CONFIRMADA aunque llegue antes de TELEFONO_ENVIADO")
    void reservaConfirmadaTransicionaAunqueElRestNoHayaAvanzado() {
        EventoAccionReserva evento = reservaConfirmadaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        assertThat(capturado.getValue().getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("AppointmentConfirmed se descarta sin lanzar cuando el reservationProcessId es desconocido")
    void reservaConfirmadaSeDescartaParaUnProcesoDesconocido() {
        EventoAccionReserva evento = reservaConfirmadaValida().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("AppointmentConfirmed se descarta sin retroceder un proceso en estado final")
    void reservaConfirmadaSeDescartaParaUnProcesoEnEstadoFinal() {
        EventoAccionReserva evento = reservaConfirmadaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CANCELADA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        useCase.procesar(evento);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    private EventoAccionReserva.EventoAccionReservaBuilder informacionAdicionalRechazadaValida() {
        return EventoAccionReserva.builder()
                .eventId("rejection-event-7d11")
                .tipo(TipoEventoAccion.INFORMACION_ADICIONAL_RECHAZADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .reason("INVALID_PHONE_NUMBER")
                .message("El telefono ingresado no es valido.")
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .schemaVersion(1);
    }

    @Test
    @DisplayName("AdditionalInformationRejected guarda el motivo y transiciona a TELEFONO_RECHAZADO")
    void informacionAdicionalRechazadaGuardaElMotivoYTransiciona() {
        EventoAccionReserva evento = informacionAdicionalRechazadaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.TELEFONO_ENVIADO).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.TELEFONO_RECHAZADO);
        assertThat(guardado.getReason()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(guardado.getReasonMessage()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(guardado.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    @DisplayName("AdditionalInformationRejected se descarta sin retroceder un proceso en estado final")
    void informacionAdicionalRechazadaSeDescartaParaUnProcesoEnEstadoFinal() {
        EventoAccionReserva evento = informacionAdicionalRechazadaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CONFIRMADA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        useCase.procesar(evento);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("AdditionalInformationRejected se descarta sin lanzar cuando el reservationProcessId es desconocido")
    void informacionAdicionalRechazadaSeDescartaParaUnProcesoDesconocido() {
        EventoAccionReserva evento = informacionAdicionalRechazadaValida().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    private EventoAccionReserva.EventoAccionReservaBuilder procesoVencidoValido() {
        return EventoAccionReserva.builder()
                .eventId("process-event-7d12")
                .tipo(TipoEventoAccion.PROCESO_VENCIDO)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .reason("HOLD_EXPIRED")
                .schemaVersion(1);
    }

    @Test
    @DisplayName("AppointmentProcessExpired guarda el motivo y cierra el proceso como VENCIDA")
    void procesoVencidoGuardaElMotivoYCierraComoVencida() {
        EventoAccionReserva evento = procesoVencidoValido().build();
        ProcesoReserva proceso = procesoDePrueba().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.VENCIDA);
        assertThat(guardado.getReason()).isEqualTo("HOLD_EXPIRED");
    }

    @Test
    @DisplayName("AppointmentProcessExpired no reabre un proceso que ya esta en estado final")
    void procesoVencidoNoReabreUnProcesoEnEstadoFinal() {
        EventoAccionReserva evento = procesoVencidoValido().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CONFIRMADA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        useCase.procesar(evento);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    private EventoAccionReserva.EventoAccionReservaBuilder procesoInvalidoValido() {
        return EventoAccionReserva.builder()
                .eventId("process-event-7d13")
                .tipo(TipoEventoAccion.PROCESO_INVALIDO)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .reason("REQUEST_EVENT_MISMATCH")
                .schemaVersion(1);
    }

    @Test
    @DisplayName("AppointmentProcessInvalid guarda el motivo y marca el proceso como INVALIDA")
    void procesoInvalidoGuardaElMotivoYMarcaComoInvalida() {
        EventoAccionReserva evento = procesoInvalidoValido().build();
        ProcesoReserva proceso = procesoDePrueba().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.INVALIDA);
        assertThat(guardado.getReason()).isEqualTo("REQUEST_EVENT_MISMATCH");
    }

    @Test
    @DisplayName("AppointmentProcessInvalid se descarta sin lanzar cuando el reservationProcessId es desconocido")
    void procesoInvalidoSeDescartaParaUnProcesoDesconocido() {
        EventoAccionReserva evento = procesoInvalidoValido().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    private EventoAccionReserva.EventoAccionReservaBuilder reservaCanceladaValida() {
        return EventoAccionReserva.builder()
                .eventId("cancellation-event-7d14")
                .tipo(TipoEventoAccion.RESERVA_CANCELADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .reason("Cambio solicitado por el usuario")
                .occurredAt(Instant.parse("2026-09-11T15:10:00Z"))
                .schemaVersion(1);
    }

    @Test
    @DisplayName("AppointmentCancelled (catedra primero): guarda cancelReason/cancelledAt y cierra como CANCELADA")
    void reservaCanceladaGuardaElMotivoYElInstanteYCierraComoCancelada() {
        EventoAccionReserva evento = reservaCanceladaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CONFIRMADA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        ProcesoReserva guardado = capturado.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoProcesoReserva.CANCELADA);
        assertThat(guardado.getCancelReason()).isEqualTo("Cambio solicitado por el usuario");
        assertThat(guardado.getCancelledAt()).isEqualTo(Instant.parse("2026-09-11T15:10:00Z"));
    }

    @Test
    @DisplayName("AppointmentCancelled admite reason null")
    void reservaCanceladaAdmiteReasonNulo() {
        EventoAccionReserva evento = reservaCanceladaValida().reason(null).build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CONFIRMADA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.procesar(evento);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        assertThat(capturado.getValue().getCancelReason()).isNull();
    }

    @Test
    @DisplayName("AppointmentCancelled (REST primero): proceso ya CANCELADA, no vuelve a aplicarse ni pisa cancelledAt")
    void reservaCanceladaNoSeReaplicaSiYaSeCancelaPorRest() {
        EventoAccionReserva evento = reservaCanceladaValida().build();
        Instant cancelledAtOriginal = Instant.parse("2026-09-11T15:09:00Z");
        ProcesoReserva proceso = procesoDePrueba()
                .estado(EstadoProcesoReserva.CANCELADA)
                .cancelledAt(cancelledAtOriginal)
                .cancelReason("Motivo original del REST")
                .build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        useCase.procesar(evento);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
        assertThat(proceso.getCancelledAt()).isEqualTo(cancelledAtOriginal);
        assertThat(proceso.getCancelReason()).isEqualTo("Motivo original del REST");
    }

    @Test
    @DisplayName("AppointmentCancelled no reabre un proceso en otro estado final (VENCIDA)")
    void reservaCanceladaNoReabreUnProcesoEnOtroEstadoFinal() {
        EventoAccionReserva evento = reservaCanceladaValida().build();
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.VENCIDA).build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        useCase.procesar(evento);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("AppointmentCancelled se descarta sin lanzar cuando el reservationProcessId es desconocido")
    void reservaCanceladaSeDescartaParaUnProcesoDesconocido() {
        EventoAccionReserva evento = reservaCanceladaValida().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

}

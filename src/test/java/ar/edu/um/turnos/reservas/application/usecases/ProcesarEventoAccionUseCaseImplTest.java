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

    private ProcesarEventoAccionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcesarEventoAccionUseCaseImpl(
                registrarEventoProcesadoPort, procesoReservaRepositoryPort, transicionarProcesoReservaUseCase);
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

}

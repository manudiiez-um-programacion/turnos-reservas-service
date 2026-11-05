package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoVencidoException;
import ar.edu.um.turnos.reservas.application.exception.TelefonoInvalidoException;
import ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.PublicarInformacionAdicionalPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnviarTelefonoUseCaseImpl")
class EnviarTelefonoUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Mock
    private PublicarInformacionAdicionalPort publicarInformacionAdicionalPort;

    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase =
            new TransicionarProcesoReservaUseCaseImpl();

    private EnviarTelefonoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new EnviarTelefonoUseCaseImpl(
                procesoReservaRepositoryPort, transicionarProcesoReservaUseCase, publicarInformacionAdicionalPort);
    }

    private ProcesoReserva.ProcesoReservaBuilder procesoDePrueba() {
        return ProcesoReserva.builder()
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .usuarioId(42L)
                .requestEventId("request-event-8a31")
                .estado(EstadoProcesoReserva.ESPERANDO_TELEFONO)
                .expiresAt(Instant.now().plusSeconds(600));
    }

    private SolicitudEnvioTelefono.SolicitudEnvioTelefonoBuilder solicitudValida() {
        return SolicitudEnvioTelefono.builder().holdId("hold-6c0c2f41").usuarioId(42L).telefono("+5492615551234");
    }

    @Test
    @DisplayName("normaliza, transiciona a TELEFONO_ENVIADO y publica para un proceso esperando el telefono")
    void enviaElTelefonoParaUnProcesoEsperandolo() {
        ProcesoReserva proceso = procesoDePrueba().build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ProcesoReserva resultado = useCase.enviar(solicitudValida().build());

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.TELEFONO_ENVIADO);
        assertThat(resultado.getTelefono()).isEqualTo("+5492615551234");

        ArgumentCaptor<InformacionAdicionalParaEnviar> capturado = ArgumentCaptor.forClass(InformacionAdicionalParaEnviar.class);
        verify(publicarInformacionAdicionalPort).publicar(capturado.capture());
        InformacionAdicionalParaEnviar publicado = capturado.getValue();
        assertThat(publicado.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(publicado.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(publicado.getRequestEventId()).isEqualTo("request-event-8a31");
        assertThat(publicado.getTelefono()).isEqualTo("+5492615551234");
    }

    @Test
    @DisplayName("tambien envia desde TELEFONO_RECHAZADO: el rechazo sigue vigente hasta expiresAt")
    void enviaElTelefonoParaUnProcesoCuyoTelefonoAnteriorFueRechazado() {
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.TELEFONO_RECHAZADO).build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ProcesoReserva resultado = useCase.enviar(solicitudValida().build());

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.TELEFONO_ENVIADO);
        verify(publicarInformacionAdicionalPort).publicar(any());
    }

    @ParameterizedTest
    @CsvSource({
            "'+54 9 261-555-1234', +5492615551234",
            "'(261) 555-1234', 2615551234",
            "'261.555.1234', 2615551234"
    })
    @DisplayName("normaliza telefonos con formato sucio (espacios, guiones, parentesis, puntos)")
    void normalizaTelefonosConFormatoSucio(String crudo, String normalizadoEsperado) {
        ProcesoReserva proceso = procesoDePrueba().build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ProcesoReserva resultado = useCase.enviar(solicitudValida().telefono(crudo).build());

        assertThat(resultado.getTelefono()).isEqualTo(normalizadoEsperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abcdefgh", "12345678901234567890", "++5492615551234"})
    @DisplayName("rechaza telefonos invalidos sin transicionar ni publicar")
    void rechazaTelefonosInvalidos(String telefonoInvalido) {
        ProcesoReserva proceso = procesoDePrueba().build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.enviar(solicitudValida().telefono(telefonoInvalido).build()))
                .isInstanceOf(TelefonoInvalidoException.class);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
        verify(publicarInformacionAdicionalPort, never()).publicar(any());
    }

    @Test
    @DisplayName("lanza HoldNoEncontradoException si el holdId no corresponde a ningun proceso local")
    void lanzaHoldNoEncontradoSiNoExiste() {
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.enviar(solicitudValida().build()))
                .isInstanceOf(HoldNoEncontradoException.class);
    }

    @Test
    @DisplayName("lanza ProcesoReservaAjenoException si el usuario autenticado no es el dueño del proceso")
    void lanzaProcesoReservaAjenoSiElUsuarioNoEsElDueño() {
        ProcesoReserva proceso = procesoDePrueba().build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.enviar(solicitudValida().usuarioId(99L).build()))
                .isInstanceOf(ProcesoReservaAjenoException.class);
    }

    @Test
    @DisplayName("lanza TransicionProcesoReservaInvalidaException si el proceso no esta esperando el telefono")
    void lanzaTransicionInvalidaSiElProcesoNoEstaEsperandoElTelefono() {
        ProcesoReserva proceso = procesoDePrueba().estado(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA).build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.enviar(solicitudValida().build()))
                .isInstanceOf(TransicionProcesoReservaInvalidaException.class);

        verify(publicarInformacionAdicionalPort, never()).publicar(any());
    }

    @Test
    @DisplayName("si expiresAt ya paso, cierra el proceso como VENCIDA sin publicar")
    void cierraElProcesoComoVencidoSiExpiresAtYaPaso() {
        ProcesoReserva proceso = procesoDePrueba().expiresAt(Instant.now().minusSeconds(1)).build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThatThrownBy(() -> useCase.enviar(solicitudValida().build()))
                .isInstanceOf(ProcesoVencidoException.class);

        ArgumentCaptor<ProcesoReserva> capturado = ArgumentCaptor.forClass(ProcesoReserva.class);
        verify(procesoReservaRepositoryPort).guardar(capturado.capture());
        assertThat(capturado.getValue().getEstado()).isEqualTo(EstadoProcesoReserva.VENCIDA);
        verify(publicarInformacionAdicionalPort, never()).publicar(any());
    }

    @Test
    @DisplayName("si ya esta vencido y en estado final, lanza sin volver a guardar (no retrocede)")
    void noRetrocedeUnProcesoYaVencidoYFinal() {
        ProcesoReserva proceso = procesoDePrueba()
                .estado(EstadoProcesoReserva.VENCIDA)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.enviar(solicitudValida().build()))
                .isInstanceOf(ProcesoVencidoException.class);

        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

}

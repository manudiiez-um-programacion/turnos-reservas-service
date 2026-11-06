package ar.edu.um.turnos.reservas.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.domain.ports.in.ConfirmarHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.ConsultarHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.ConsultarReservasUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.CrearHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;

@ExtendWith(MockitoExtension.class)
class ReservasServiceTest {

    @Mock
    private ProcesarEventoAccionUseCase procesarEventoAccionUseCase;

    @Mock
    private TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;

    @Mock
    private CrearHoldUseCase crearHoldUseCase;

    @Mock
    private ConfirmarHoldUseCase confirmarHoldUseCase;

    @Mock
    private ConsultarHoldUseCase consultarHoldUseCase;

    @Mock
    private EnviarTelefonoUseCase enviarTelefonoUseCase;

    @Mock
    private ConsultarReservasUseCase consultarReservasUseCase;

    private ReservasService crearService() {
        return new ReservasService(procesarEventoAccionUseCase, transicionarProcesoReservaUseCase, crearHoldUseCase,
                confirmarHoldUseCase, consultarHoldUseCase, enviarTelefonoUseCase, consultarReservasUseCase);
    }

    @Test
    void delegaEnElCasoDeUsoDeProcesarEvento() {
        ReservasService service = crearService();
        EventoAccionReserva evento = EventoAccionReserva.builder().eventId("un-evento").build();

        service.procesarEventoAccion(evento);

        verify(procesarEventoAccionUseCase).procesar(evento);
    }

    @Test
    void delegaEnElCasoDeUsoDeTransicionar() {
        ReservasService service = crearService();
        ProcesoReserva proceso = ProcesoReserva.builder().reservationProcessId("process-1").build();
        Instant ocurridoEn = Instant.parse("2026-09-10T14:36:00Z");

        service.transicionar(proceso, EstadoProcesoReserva.CONFIRMADA, ocurridoEn);

        verify(transicionarProcesoReservaUseCase).transicionar(proceso, EstadoProcesoReserva.CONFIRMADA, ocurridoEn);
    }

    @Test
    void delegaEnElCasoDeUsoDeCrearHold() {
        ReservasService service = crearService();
        SolicitudHold solicitud = SolicitudHold.builder().professionalId(15L).build();
        ProcesoReserva esperado = ProcesoReserva.builder().holdId("hold-1").build();
        when(crearHoldUseCase.crear(solicitud)).thenReturn(esperado);

        assertThat(service.crearHold(solicitud)).isSameAs(esperado);
    }

    @Test
    void delegaEnElCasoDeUsoDeConfirmarHold() {
        ReservasService service = crearService();
        SolicitudConfirmacionHold solicitud = SolicitudConfirmacionHold.builder().holdId("hold-1").build();
        ProcesoReserva esperado = ProcesoReserva.builder().holdId("hold-1").build();
        when(confirmarHoldUseCase.confirmar(solicitud)).thenReturn(esperado);

        assertThat(service.confirmarHold(solicitud)).isSameAs(esperado);
    }

    @Test
    void delegaEnElCasoDeUsoDeConsultarHold() {
        ReservasService service = crearService();
        ProcesoReserva esperado = ProcesoReserva.builder().holdId("hold-1").build();
        when(consultarHoldUseCase.consultar("hold-1", 7L)).thenReturn(esperado);

        assertThat(service.consultarHold("hold-1", 7L)).isSameAs(esperado);
    }

    @Test
    void delegaEnElCasoDeUsoDeEnviarTelefono() {
        ReservasService service = crearService();
        SolicitudEnvioTelefono solicitud = SolicitudEnvioTelefono.builder().holdId("hold-1").build();
        ProcesoReserva esperado = ProcesoReserva.builder().holdId("hold-1").build();
        when(enviarTelefonoUseCase.enviar(solicitud)).thenReturn(esperado);

        assertThat(service.enviarTelefono(solicitud)).isSameAs(esperado);
    }

    @Test
    void delegaEnElCasoDeUsoDeConsultarReservas() {
        ReservasService service = crearService();
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder().usuarioId(7L).build();
        PaginaProcesoReserva esperado = new PaginaProcesoReserva(List.of(), 0L);
        when(consultarReservasUseCase.consultar(filtro)).thenReturn(esperado);

        assertThat(service.consultarReservas(filtro)).isSameAs(esperado);
    }

}

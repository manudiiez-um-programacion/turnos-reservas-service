package ar.edu.um.turnos.reservas.application.service;

import java.time.Instant;
import org.springframework.stereotype.Service;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;
import ar.edu.um.turnos.reservas.domain.ports.in.ConfirmarHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.ConsultarHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.CrearHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice {@code reservas}: es lo unico que la infraestructura
 * (el oyente de Kafka y el controlador REST de holds y reservas) conoce.
 * Nadie fuera de este paquete habla con un caso de uso ni con un puerto de
 * salida directamente.
 */
@Service
@RequiredArgsConstructor
public class ReservasService {

    private final ProcesarEventoAccionUseCase procesarEventoAccionUseCase;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;
    private final CrearHoldUseCase crearHoldUseCase;
    private final ConfirmarHoldUseCase confirmarHoldUseCase;
    private final ConsultarHoldUseCase consultarHoldUseCase;
    private final EnviarTelefonoUseCase enviarTelefonoUseCase;

    public void procesarEventoAccion(EventoAccionReserva evento) {
        procesarEventoAccionUseCase.procesar(evento);
    }

    public ProcesoReserva transicionar(ProcesoReserva proceso, EstadoProcesoReserva nuevoEstado, Instant ocurridoEn) {
        return transicionarProcesoReservaUseCase.transicionar(proceso, nuevoEstado, ocurridoEn);
    }

    public ProcesoReserva crearHold(SolicitudHold solicitud) {
        return crearHoldUseCase.crear(solicitud);
    }

    public ProcesoReserva confirmarHold(SolicitudConfirmacionHold solicitud) {
        return confirmarHoldUseCase.confirmar(solicitud);
    }

    public ProcesoReserva consultarHold(String holdId, Long usuarioId) {
        return consultarHoldUseCase.consultar(holdId, usuarioId);
    }

    public ProcesoReserva enviarTelefono(SolicitudEnvioTelefono solicitud) {
        return enviarTelefonoUseCase.enviar(solicitud);
    }

}

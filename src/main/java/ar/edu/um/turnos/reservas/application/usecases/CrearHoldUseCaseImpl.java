package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.HoldCreado;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProfesionalParaHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.domain.model.TransicionProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.CrearHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.CrearHoldCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ValidarSlotCatalogoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso (#27): valida el slot contra la agenda
 * vigente del catalogo, pide el hold a la catedra y persiste el proceso de
 * reserva nuevo en {@link EstadoProcesoReserva#HOLD_CREADO}.
 *
 * <p>No pasa por {@link ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase}:
 * esa maquina de estados valida transiciones entre estados de un proceso
 * existente, y {@code HOLD_CREADO} es el estado inicial del proceso, no el
 * destino de una transicion.
 *
 * <p>No reintenta ante un timeout de la catedra (§18.4): todavia no existe
 * {@code holdId}/{@code reservationProcessId} para reconciliar contra la
 * catedra, así que un timeout de este POST simplemente se propaga. La
 * reconciliacion que #36 agrega aquí es local y previa a la llamada: si ya
 * hay un proceso vigente (no final, no vencido) para el mismo profesional,
 * fecha, hora y usuario —por ejemplo porque la respuesta del primer intento
 * se perdio pero igual se persistio, y el cliente reintento todo el
 * request— se devuelve ese proceso en vez de pedir un segundo hold.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrearHoldUseCaseImpl implements CrearHoldUseCase {

    private final ValidarSlotCatalogoPort validarSlotCatalogoPort;
    private final CrearHoldCatedraPort crearHoldCatedraPort;
    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Override
    public ProcesoReserva crear(SolicitudHold solicitud) {
        // HoldWebMapper.toSolicitud puede devolver null (null-guarda su
        // entrada, como todos los mappers del proyecto): este chequeo evita
        // que ese null llegue sin control al resto del metodo.
        Objects.requireNonNull(solicitud, "la solicitud de hold no puede ser null");

        Optional<ProcesoReserva> vigente = procesoReservaRepositoryPort.buscarVigentePorSlot(
                solicitud.usuarioId(), solicitud.professionalId(), solicitud.fecha(), solicitud.horaInicio(), Instant.now());
        if (vigente.isPresent()) {
            log.info("Ya existe el proceso vigente {} para el profesional {} el {} {} del usuario {}: se reutiliza en vez de crear un hold nuevo",
                    vigente.get().getReservationProcessId(), solicitud.professionalId(), solicitud.fecha(),
                    solicitud.horaInicio(), solicitud.usuarioId());
            return vigente.get();
        }

        ProfesionalParaHold profesional = validarSlotCatalogoPort.validar(
                solicitud.professionalId(), solicitud.fecha(), solicitud.horaInicio());

        HoldCreado holdCreado = crearHoldCatedraPort.crear(solicitud);

        List<TransicionProcesoReserva> historial = new ArrayList<>();
        historial.add(new TransicionProcesoReserva(EstadoProcesoReserva.HOLD_CREADO, Instant.now()));

        ProcesoReserva proceso = ProcesoReserva.builder()
                .reservationProcessId(holdCreado.reservationProcessId())
                .holdId(holdCreado.holdId())
                .usuarioId(solicitud.usuarioId())
                .externalPatientId(solicitud.externalPatientId())
                .professionalId(solicitud.professionalId())
                .professionalNombreHistorico(profesional.nombre())
                .professionalApellidoHistorico(profesional.apellido())
                .fecha(solicitud.fecha())
                .horaInicio(solicitud.horaInicio())
                .estado(EstadoProcesoReserva.HOLD_CREADO)
                .expiresAt(holdCreado.expiresAt())
                .historial(historial)
                .build();

        log.info("Hold {} creado para el proceso {} (profesional {}, usuario {})",
                holdCreado.holdId(), holdCreado.reservationProcessId(),
                solicitud.professionalId(), solicitud.usuarioId());
        return procesoReservaRepositoryPort.guardar(proceso);
    }

}

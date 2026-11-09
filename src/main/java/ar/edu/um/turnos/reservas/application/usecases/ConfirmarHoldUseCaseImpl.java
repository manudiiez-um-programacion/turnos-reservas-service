package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.reservas.application.exception.ExternalPatientMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.HoldOwnerMismatchException;
import ar.edu.um.turnos.reservas.application.exception.HoldVencidoException;
import ar.edu.um.turnos.reservas.application.exception.HoldYaConfirmadoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.domain.model.ConfirmacionHoldCatedra;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;
import ar.edu.um.turnos.reservas.domain.model.SolicitudConfirmacionHold;
import ar.edu.um.turnos.reservas.domain.ports.in.ConfirmarHoldUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ConfirmarHoldCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ConsultarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso (#28): valida que el proceso exista y sea
 * del usuario, lo marca {@link EstadoProcesoReserva#CONFIRMACION_INICIAL_ENVIADA}
 * antes de llamar a la catedra —para poder reconciliar un timeout (#36) en
 * vez de reintentar a ciegas (§18.4)— y aplica el resultado.
 *
 * <p>{@code RESERVATION_PROCESS_ALREADY_CONFIRMED} ({@link HoldYaConfirmadoException})
 * se trata como exito idempotente (§18.4): tratarlo como error nuevo seria
 * el bug que esa seccion advierte. {@code HOLD_EXPIRED} y {@code HOLD_NOT_FOUND}
 * cierran el proceso local (§18.3): un evento tardio no puede reabrirlo.
 * {@code HOLD_OWNER_MISMATCH} y {@code EXTERNAL_PATIENT_MISMATCH} se
 * registran como inconsistencia y se propagan sin tocar el estado local.
 *
 * <p>Un timeout o un error de infraestructura ({@code CatedraServerErrorException}
 * o {@code ResourceAccessException}, nunca reintentados a ciegas por
 * {@code CatedraClient}) se reconcilian antes de repetir la llamada (#36,
 * §18.4): se busca el {@code reservationProcessId} en
 * {@code GET /api/appointments} filtrado por {@code externalPatientId}. Su
 * sola presencia ahí ya prueba que la catedra creo la reserva
 * {@code PHONE_PENDING} —el confirm nunca se queda a medias del lado de
 * ellos—, así que alcanza para alinear el estado local sin repetir la
 * llamada. Si todavia no esta, se permite exactamente un reintento acotado;
 * si ese reintento tambien da timeout, se propaga sin insistir mas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmarHoldUseCaseImpl implements ConfirmarHoldUseCase {

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final ConfirmarHoldCatedraPort confirmarHoldCatedraPort;
    private final ConsultarReservaCatedraPort consultarReservaCatedraPort;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;

    @Override
    public ProcesoReserva confirmar(SolicitudConfirmacionHold solicitud) {
        Objects.requireNonNull(solicitud, "la solicitud de confirmacion no puede ser null");

        ProcesoReserva proceso = procesoReservaRepositoryPort.buscarPorHoldId(solicitud.holdId())
                .orElseThrow(() -> new HoldNoEncontradoException(solicitud.holdId()));

        if (!proceso.getUsuarioId().equals(solicitud.usuarioId())) {
            throw new ProcesoReservaAjenoException(solicitud.holdId(), solicitud.usuarioId());
        }

        if (proceso.getEstado() == EstadoProcesoReserva.ESPERANDO_TELEFONO) {
            log.info("Confirmacion repetida para el hold {}: el proceso {} ya esta esperando el telefono",
                    solicitud.holdId(), proceso.getReservationProcessId());
            return proceso;
        }

        if (proceso.getEstado() != EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA) {
            proceso = transicionarProcesoReservaUseCase.transicionar(
                    proceso, EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA, Instant.now());
            proceso = procesoReservaRepositoryPort.guardar(proceso);
        }

        return intentarConfirmar(proceso, solicitud, true);
    }

    private ProcesoReserva intentarConfirmar(ProcesoReserva proceso, SolicitudConfirmacionHold solicitud, boolean permitirReconciliacion) {
        try {
            ConfirmacionHoldCatedra confirmacion = confirmarHoldCatedraPort.confirmar(
                    proceso, solicitud.externalPatientId(), solicitud.patientFirstName(), solicitud.patientLastName());
            proceso.setExpiresAt(confirmacion.expiresAt());
            return avanzarAEsperandoTelefono(proceso);
        } catch (HoldYaConfirmadoException yaConfirmado) {
            log.info("La catedra ya habia procesado la confirmacion del hold {}: se trata como exito idempotente",
                    solicitud.holdId());
            return avanzarAEsperandoTelefono(proceso);
        } catch (HoldVencidoException vencido) {
            cerrarProceso(proceso, EstadoProcesoReserva.VENCIDA);
            throw vencido;
        } catch (HoldNoEncontradoException noEncontrado) {
            cerrarProceso(proceso, EstadoProcesoReserva.INVALIDA);
            throw noEncontrado;
        } catch (HoldOwnerMismatchException | ExternalPatientMismatchException inconsistencia) {
            log.warn("Inconsistencia confirmando el hold {}: {}", solicitud.holdId(), inconsistencia.getMessage());
            throw inconsistencia;
        } catch (CatedraServerErrorException | ResourceAccessException timeout) {
            if (!permitirReconciliacion) {
                log.warn("Reintento de la confirmacion del hold {} agotado tras reconciliar: {}",
                        solicitud.holdId(), timeout.getMessage());
                throw timeout;
            }
            return reconciliarYReintentarConfirmacion(proceso, solicitud, timeout);
        }
    }

    private ProcesoReserva reconciliarYReintentarConfirmacion(
            ProcesoReserva proceso, SolicitudConfirmacionHold solicitud, RuntimeException timeout) {
        log.warn("Timeout o error de infraestructura confirmando el hold {}: se reconcilia antes de reintentar ({})",
                solicitud.holdId(), timeout.getMessage());

        Optional<ReservaCatedraEncontrada> encontrada = buscarEnLaCatedra(solicitud.externalPatientId(), proceso.getReservationProcessId());
        if (encontrada.isPresent()) {
            log.info("La catedra ya tenia aplicada la confirmacion del hold {} (status={}): se alinea el estado local sin repetir la llamada",
                    solicitud.holdId(), encontrada.get().status());
            return avanzarAEsperandoTelefono(proceso);
        }

        log.info("La catedra todavia no tiene la confirmacion del hold {}: se reintenta una vez, acotado", solicitud.holdId());
        return intentarConfirmar(proceso, solicitud, false);
    }

    private Optional<ReservaCatedraEncontrada> buscarEnLaCatedra(String externalPatientId, String reservationProcessId) {
        try {
            return consultarReservaCatedraPort.buscarPorReservationProcessId(externalPatientId, reservationProcessId);
        } catch (CatedraServerErrorException | ResourceAccessException fallo) {
            log.warn("No se pudo reconciliar el proceso {} contra la catedra: {}", reservationProcessId, fallo.getMessage());
            return Optional.empty();
        }
    }

    private ProcesoReserva avanzarAEsperandoTelefono(ProcesoReserva proceso) {
        ProcesoReserva actualizado = transicionarProcesoReservaUseCase.transicionar(
                proceso, EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now());
        return procesoReservaRepositoryPort.guardar(actualizado);
    }

    private void cerrarProceso(ProcesoReserva proceso, EstadoProcesoReserva estadoFinal) {
        ProcesoReserva cerrado = transicionarProcesoReservaUseCase.transicionar(proceso, estadoFinal, Instant.now());
        procesoReservaRepositoryPort.guardar(cerrado);
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.LimiteReintentosTelefonoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoVencidoException;
import ar.edu.um.turnos.reservas.application.exception.TelefonoInvalidoException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudEnvioTelefono;
import ar.edu.um.turnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.PublicarInformacionAdicionalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso (#30, §15.5 de la referencia): valida la
 * propiedad del proceso, normaliza y valida el telefono con el mismo
 * criterio que la catedra, verifica que el plazo para responder no haya
 * pasado, y solo entonces transiciona y publica.
 *
 * <p>El telefono se persiste y se transiciona a
 * {@link EstadoProcesoReserva#TELEFONO_ENVIADO} antes de publicar, no
 * despues: el mismo motivo que #28 (poder reconciliar un timeout de Kafka
 * por {@code reservationProcessId} en lugar de reintentar a ciegas, §18.4).
 *
 * <p>{@link EstadoProcesoReserva#puedeTransicionarA} ya exige exactamente
 * {@link EstadoProcesoReserva#ESPERANDO_TELEFONO} o
 * {@link EstadoProcesoReserva#TELEFONO_RECHAZADO} como origen de
 * {@code TELEFONO_ENVIADO}: no hace falta repetir ese chequeo a mano, el
 * intento de transicion ya lo aplica y lanza si el proceso esta en
 * cualquier otro estado.
 *
 * <p>{@code MAX_INTENTOS_TELEFONO} limita los reintentos (#31, §18.4 de la
 * referencia): valor fijo en el codigo y no en una variable de entorno,
 * mismo criterio que los reintentos del oyente de Kafka (#18), porque es un
 * ajuste operativo propio y no un dato que entregue la catedra. Alcanzado el
 * limite, el proceso sigue vivo hasta que la catedra lo cierre con su propio
 * {@code AppointmentProcessExpired} (#31, {@code ProcesarEventoAccionUseCaseImpl});
 * esta clase solo corta nuevos intentos locales antes de eso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnviarTelefonoUseCaseImpl implements EnviarTelefonoUseCase {

    private static final int MAX_INTENTOS_TELEFONO = 3;

    // Mismo criterio que la catedra (§15.5): primero quita espacios,
    // guiones, parentesis y puntos, despues valida el resultado.
    private static final Pattern CARACTERES_A_QUITAR = Pattern.compile("[\\s\\-().]");
    private static final Pattern TELEFONO_VALIDO = Pattern.compile("^\\+?[0-9]{7,15}$");

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;
    private final PublicarInformacionAdicionalPort publicarInformacionAdicionalPort;

    @Override
    public ProcesoReserva enviar(SolicitudEnvioTelefono solicitud) {
        Objects.requireNonNull(solicitud, "la solicitud de envio de telefono no puede ser null");

        ProcesoReserva proceso = procesoReservaRepositoryPort.buscarPorHoldId(solicitud.holdId())
                .orElseThrow(() -> new HoldNoEncontradoException(solicitud.holdId()));

        if (!proceso.getUsuarioId().equals(solicitud.usuarioId())) {
            throw new ProcesoReservaAjenoException(solicitud.holdId(), solicitud.usuarioId());
        }

        String telefonoNormalizado = normalizar(solicitud.telefono());

        if (haVencido(proceso)) {
            cerrarComoVencidoSiNoEsFinal(proceso);
            throw new ProcesoVencidoException(proceso.getReservationProcessId());
        }

        if (proceso.getIntentosTelefono() >= MAX_INTENTOS_TELEFONO) {
            throw new LimiteReintentosTelefonoException(proceso.getReservationProcessId(), MAX_INTENTOS_TELEFONO);
        }

        proceso.setTelefono(telefonoNormalizado);
        proceso.setIntentosTelefono(proceso.getIntentosTelefono() + 1);
        proceso = transicionarProcesoReservaUseCase.transicionar(
                proceso, EstadoProcesoReserva.TELEFONO_ENVIADO, Instant.now());
        proceso = procesoReservaRepositoryPort.guardar(proceso);

        publicarInformacionAdicionalPort.publicar(InformacionAdicionalParaEnviar.builder()
                .reservationProcessId(proceso.getReservationProcessId())
                .holdId(proceso.getHoldId())
                .requestEventId(proceso.getRequestEventId())
                .telefono(telefonoNormalizado)
                .build());

        log.info("Telefono enviado para el proceso {} (holdId={})",
                proceso.getReservationProcessId(), proceso.getHoldId());
        return proceso;
    }

    private boolean haVencido(ProcesoReserva proceso) {
        return proceso.getExpiresAt() != null && proceso.getExpiresAt().isBefore(Instant.now());
    }

    private void cerrarComoVencidoSiNoEsFinal(ProcesoReserva proceso) {
        if (proceso.getEstado().esFinal()) {
            return;
        }
        ProcesoReserva cerrado = transicionarProcesoReservaUseCase.transicionar(
                proceso, EstadoProcesoReserva.VENCIDA, Instant.now());
        procesoReservaRepositoryPort.guardar(cerrado);
    }

    private String normalizar(String crudo) {
        if (crudo == null) {
            throw new TelefonoInvalidoException();
        }
        String limpio = CARACTERES_A_QUITAR.matcher(crudo).replaceAll("");
        if (!TELEFONO_VALIDO.matcher(limpio).matches()) {
            throw new TelefonoInvalidoException();
        }
        return limpio;
    }

}

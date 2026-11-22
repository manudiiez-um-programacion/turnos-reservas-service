package ar.edu.um.turnos.reservas.infrastructure.messaging.adapter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.CatedraProperties;
import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;
import ar.edu.um.turnos.reservas.domain.ports.out.PublicarInformacionAdicionalPort;
import ar.edu.um.turnos.reservas.infrastructure.messaging.dto.InformacionAdicionalSubmittedMensaje;
import ar.edu.um.turnos.reservas.infrastructure.messaging.exception.PublicacionInformacionAdicionalException;
import ar.edu.um.turnos.reservas.infrastructure.messaging.mapper.InformacionAdicionalMessagingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptador de {@link PublicarInformacionAdicionalPort} (#19): publica en
 * {@code catedra.turnos.telefono.{groupId}} y espera la confirmacion del
 * broker antes de darle el envio por exitoso.
 *
 * <p>El teléfono nunca aparece en estos logs: solo se registran el
 * {@code eventId} propio y los identificadores de correlacion
 * ({@code reservationProcessId}, {@code holdId}), nunca el payload completo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InformacionAdicionalKafkaAdapter implements PublicarInformacionAdicionalPort {

    // Ajuste operativo, no un dato que entregue la catedra: cuanto se espera
    // la confirmacion del broker antes de tratar el envio como fallido.
    private static final long TIMEOUT_ENVIO_SEGUNDOS = 5L;

    private final KafkaTemplate<String, String> telefonoKafkaTemplate;
    private final InformacionAdicionalMessagingMapper informacionAdicionalMessagingMapper;
    private final CatedraProperties catedraProperties;

    @Override
    public void publicar(InformacionAdicionalParaEnviar informacion) {
        InformacionAdicionalSubmittedMensaje mensaje =
                informacionAdicionalMessagingMapper.aMensaje(informacion, catedraProperties.groupId());
        String json = informacionAdicionalMessagingMapper.aJson(mensaje);
        String topic = catedraProperties.kafka().appointmentPhoneTopic();
        String key = informacion.getReservationProcessId();

        try {
            telefonoKafkaTemplate.send(topic, key, json).get(TIMEOUT_ENVIO_SEGUNDOS, TimeUnit.SECONDS);
            log.info("AdditionalInformationSubmitted publicado (eventId={}, reservationProcessId={}, holdId={})",
                    mensaje.eventId(), key, mensaje.holdId());
        } catch (ExecutionException | TimeoutException | InterruptedException fallo) {
            if (fallo instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("No se pudo confirmar la publicacion de AdditionalInformationSubmitted "
                            + "(eventId={}, reservationProcessId={}, holdId={})",
                    mensaje.eventId(), key, mensaje.holdId(), fallo);
            throw new PublicacionInformacionAdicionalException(
                    "no se pudo confirmar la publicacion de AdditionalInformationSubmitted", fallo);
        }
    }

}

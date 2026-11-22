package ar.edu.um.turnos.reservas.infrastructure.messaging.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import ar.edu.um.turnos.config.CatedraProperties;
import lombok.RequiredArgsConstructor;

/**
 * Arma el consumidor de {@code alumnos.turnos.acciones.{groupId}} (#18):
 * bootstrap servers y consumer group desde {@link CatedraProperties#kafka()},
 * con la politica de confirmacion de offset y de reintentos que exige el
 * issue.
 *
 * <p><strong>{@code AckMode.RECORD}</strong>: Spring confirma el offset recien
 * despues de que el metodo del oyente retorna sin lanzar, nunca antes (§16 de
 * la referencia). Como el oyente solo acaba de retornar tras invocar al caso
 * de uso, el commit queda despues de "persistir el efecto", aunque hoy ese
 * efecto sea unicamente el registro observable: #29, #31 y #35 agregan
 * persistencia real dentro del mismo caso de uso sin tener que tocar esta
 * configuracion.
 *
 * <p><strong>Reintentos acotados</strong>: un evento que no se puede procesar
 * (JSON invalido, falta un campo comun obligatorio) se reintenta un numero
 * fijo de veces con espera fija y despues se registra el fallo y se avanza,
 * para no bloquear la particion indefinidamente (§18.4). Los valores son
 * ajuste operativo, no un dato que entregue la catedra, por eso estan fijos y
 * no en variables de entorno.
 */
@Configuration
@RequiredArgsConstructor
public class AccionesReservaKafkaConsumerConfig {

    private static final long REINTENTO_BACKOFF_MS = 500L;
    private static final long REINTENTOS_MAXIMOS = 3L;

    private final CatedraProperties catedraProperties;

    @Bean
    ConsumerFactory<String, String> accionesReservaConsumerFactory() {
        CatedraProperties.Kafka kafka = catedraProperties.kafka();

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers());
        propiedades.put(ConsumerConfig.GROUP_ID_CONFIG, kafka.consumerGroupId());
        propiedades.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedades.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propiedades.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 'earliest' y no el default de Kafka ('latest'): si todavia no hay
        // offset confirmado para este consumer group (primera vez que arranca,
        // o se reprovisiono), el consumidor arranca desde el principio del
        // topic en lugar de saltarse lo que ya estaba publicado.
        propiedades.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(propiedades);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> accionesReservaKafkaListenerContainerFactory(
            ConsumerFactory<String, String> accionesReservaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(accionesReservaConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(REINTENTO_BACKOFF_MS, REINTENTOS_MAXIMOS)));
        return factory;
    }

}

package ar.edu.um.turnos.reservas.infrastructure.messaging.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import ar.edu.um.turnos.config.CatedraProperties;
import lombok.RequiredArgsConstructor;

/**
 * Arma el productor de {@code catedra.turnos.telefono.{groupId}} (#19):
 * bootstrap servers desde {@link CatedraProperties#kafka()}, igual que el
 * consumidor de #18.
 *
 * <p><strong>{@code acks=all}</strong>: el issue exige que el envio quede
 * confirmado antes de darlo por exitoso (§15.5 de la referencia no admite
 * reintentos silenciosos del lado de la catedra). Con {@code acks=all} el
 * broker lider espera la replicacion antes de confirmar, asi que una
 * confirmacion recibida es una garantia real y no solo "se encolo".
 */
@Configuration
@RequiredArgsConstructor
public class TelefonoKafkaProducerConfig {

    private final CatedraProperties catedraProperties;

    @Bean
    ProducerFactory<String, String> telefonoProducerFactory() {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, catedraProperties.kafka().bootstrapServers());
        propiedades.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propiedades.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propiedades.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(propiedades);
    }

    @Bean
    KafkaTemplate<String, String> telefonoKafkaTemplate(ProducerFactory<String, String> telefonoProducerFactory) {
        return new KafkaTemplate<>(telefonoProducerFactory);
    }

}

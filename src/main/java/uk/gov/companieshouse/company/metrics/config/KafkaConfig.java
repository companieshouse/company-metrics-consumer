package uk.gov.companieshouse.company.metrics.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import uk.gov.companieshouse.company.metrics.serialization.ChsDeltaDeserializer;
import uk.gov.companieshouse.delta.ChsDelta;


@Configuration
@Profile("!test")
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Kafka Consumer Factory Message.
     */
    @Bean
    public ConsumerFactory<String, ChsDelta> consumerFactoryMessage() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                new ChsDeltaDeserializer());
    }

    /**
     * Kafka Listener Container Factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChsDelta> listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChsDelta> factory
                = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryMessage());
        return factory;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ChsDeltaDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

}



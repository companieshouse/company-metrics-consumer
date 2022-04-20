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
import uk.gov.companieshouse.company.metrics.service.api.serialization.ResourceChangedDataDeserializer;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Configuration
@Profile("!test")
public class KafkaConfig {

    private String bootstrapServers;


    private final ResourceChangedDataDeserializer resourceChangedDataDeserializer;

    /**
     * Kafka Consumer Factory Message.
     */
    public KafkaConfig(ResourceChangedDataDeserializer resourceChangedDataDeserializer,
                       @Value("${spring.kafka"
            + ".bootstrap-servers}") String bootstrapServers) {
        this.resourceChangedDataDeserializer = resourceChangedDataDeserializer;
        this.bootstrapServers = bootstrapServers;
    }


    /**
     * Kafka Consumer Factory Message.
     */
    @Bean
    public ConsumerFactory<String, ResourceChangedData> consumerFactoryMessage() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                resourceChangedDataDeserializer);
    }

    /**
     * Kafka Listener Container Factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ResourceChangedData>
             listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ResourceChangedData> factory
                = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryMessage());
        return factory;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ResourceChangedDataDeserializer.class);

        return props;
    }
}



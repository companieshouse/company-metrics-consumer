package uk.gov.companieshouse.company.metrics.config;

import static uk.gov.companieshouse.company.metrics.Application.APPLICATION_NAME_SPACE;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import uk.gov.companieshouse.company.metrics.exception.RetryableTopicErrorInterceptor;
import uk.gov.companieshouse.company.metrics.serialization.ResourceChangedDataDeserializer;
import uk.gov.companieshouse.company.metrics.serialization.ResourceChangedDataSerializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Configuration
@Profile("!test")
public class KafkaConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final ResourceChangedDataDeserializer resourceChangedDataDeserializer;
    private final ResourceChangedDataSerializer resourceChangedDataSerializer;
    private final String bootstrapServers;
    private final Integer listenerConcurrency;

    /**
     * Kafka Consumer Factory Message.
     */
    public KafkaConfig(ResourceChangedDataDeserializer resourceChangedDataDeserializer,
                       ResourceChangedDataSerializer resourceChangedDataSerializer,
                       @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                       @Value("${spring.kafka.listener.concurrency}") Integer listenerConcurrency) {
        this.resourceChangedDataDeserializer = resourceChangedDataDeserializer;
        this.resourceChangedDataSerializer = resourceChangedDataSerializer;
        this.bootstrapServers = bootstrapServers;
        this.listenerConcurrency = listenerConcurrency;
    }

    private Map<String, Object> consumerConfigs() {
        LOGGER.info("consumerConfigs(bootstrapServers=%s) method called.".formatted(bootstrapServers));

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, ResourceChangedDataDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return props;
    }

    /**
     * Kafka Consumer Factory Message.
     */
    @Bean
    public ConsumerFactory<@NonNull String, ResourceChangedData> consumerFactory() {
        LOGGER.info("consumerFactory() method called.");

        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                new ErrorHandlingDeserializer<>(resourceChangedDataDeserializer));
    }

    /**
     * Kafka Listener Container Factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull ResourceChangedData> listenerContainerFactory(
            ConsumerFactory<@NonNull String, ResourceChangedData> consumerFactory) {
        LOGGER.info("listenerContainerFactory(listeners=%d) method called.".formatted(consumerFactory.getListeners().size()));

        ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull ResourceChangedData> factory
                = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(listenerConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

    /**
     * Kafka Producer Factory.
     */
    @Bean
    public ProducerFactory<@NonNull String, Object> producerFactory() {
        LOGGER.info("producerFactory() method called.");

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResourceChangedDataSerializer.class);
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, RetryableTopicErrorInterceptor.class.getName());

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), resourceChangedDataSerializer);
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate(ProducerFactory<@NonNull String, Object> producerFactory) {
        LOGGER.info("kafkaTemplate(listeners=%d) method called.".formatted(producerFactory.getListeners().size()));

        return new KafkaTemplate<>(producerFactory);
    }

}



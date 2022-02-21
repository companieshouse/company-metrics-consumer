package uk.gov.companieshouse.company.metrics.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.company.metrics.serialization.ChsDeltaSerializer;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.company.metrics.serialization.ChsDeltaDeserializer;

@TestConfiguration
public class KafkaTestContainerConfig {

    @Bean
    public KafkaContainer kafkaContainer() {
        KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
        kafkaContainer.start();
        return kafkaContainer;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<Integer, String> listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Integer, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<Integer, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(kafkaContainer()));
    }

    @Bean
    public Map<String, Object> consumerConfigs(KafkaContainer kafkaContainer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "company-metrics-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ChsDeltaDeserializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, ChsDelta> producerFactory(KafkaContainer kafkaContainer) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ChsDeltaSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ChsDelta> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory(kafkaContainer()));
    }

}

package uk.gov.companieshouse.company.metrics.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.companieshouse.company.metrics.consumer.ResettableCountDownLatch;

import java.util.HashMap;
import java.util.UUID;

@TestConfiguration
public class TestConfig {

    @Bean
    public ResettableCountDownLatch resettableCountDownLatch() {
        return new ResettableCountDownLatch();
    }

    @Bean
    KafkaConsumer<String, byte[]> testConsumer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaConsumer<>(new HashMap<>() {{
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        }}, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    KafkaProducer<String, byte[]> testProducer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaProducer<>(new HashMap<>() {{
            put(ProducerConfig.ACKS_CONFIG, "all");
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        }}, new StringSerializer(), new ByteArraySerializer());
    }
}
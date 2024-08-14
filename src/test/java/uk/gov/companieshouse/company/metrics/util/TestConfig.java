package uk.gov.companieshouse.company.metrics.util;

import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_CHARGES_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_TOPIC;

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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@TestConfiguration
public class TestConfig {

    @Bean
    KafkaConsumer<String, byte[]> testConsumer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(new HashMap<>() {{
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        }}, new StringDeserializer(), new ByteArrayDeserializer());

        consumer.subscribe(List.of(
                STREAM_COMPANY_CHARGES_TOPIC,
                STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_RETRY_TOPIC,
                STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_ERROR_TOPIC,
                STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_INVALID_TOPIC,
                STREAM_COMPANY_OFFICERS_TOPIC,
                STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC,
                STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC,
                STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC,
                STREAM_PSC_STATEMENTS_TOPIC,
                STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC,
                STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC,
                STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC
        ));

        return consumer;
    }

    @Bean
    KafkaProducer<String, byte[]> testProducer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaProducer<>(new HashMap<>() {{
            put(ProducerConfig.ACKS_CONFIG, "all");
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        }}, new StringSerializer(), new ByteArraySerializer());
    }
}
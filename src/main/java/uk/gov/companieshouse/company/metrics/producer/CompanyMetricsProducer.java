package uk.gov.companieshouse.company.metrics.producer;

import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;

@Component
public class CompanyMetricsProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyMetricsProducer.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer-timeout}")
    private Integer timeoutMilliSeconds;

    @Value("${spring.kafka.producer-retries}")
    private Integer retries;

    private CHKafkaProducer chKafkaProducer;

    /**
     * Post construct init.
     */
    @PostConstruct
    public void init() {
        LOGGER.debug("Configuring CH Kafka producer");
        final ProducerConfig config = createProducerConfig();
        config.setRoundRobinPartitioner(true);
        config.setAcks(Acks.WAIT_FOR_ALL);
        config.setRetries(retries);
        config.setRequestTimeoutMilliseconds(timeoutMilliSeconds);
        chKafkaProducer = new CHKafkaProducer(config);
    }

    /**
     * Send Kafka message.
     */
    public void send(Message message) {
        try {
            chKafkaProducer.send(message);
        } catch (ExecutionException | InterruptedException ex) {
            LOGGER.error("Error while sending the message", ex);
        }
    }

    protected ProducerConfig createProducerConfig() {
        final ProducerConfig config = new ProducerConfig();
        config.setBrokerAddresses(bootstrapServers.split(","));
        return config;
    }
}

package uk.gov.companieshouse.company.metrics.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.processor.CompanyMetricsProcessor;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Component
public class CompanyMetricsConsumer {

    private final CompanyMetricsProcessor metricsProcessor;
    private final uk.gov.companieshouse.logging.Logger logger;

    @Autowired
    public CompanyMetricsConsumer(CompanyMetricsProcessor metricsProcessor,
                                  uk.gov.companieshouse.logging.Logger logger) {
        this.metricsProcessor = metricsProcessor;
        this.logger = logger;
    }

    /**
     * Receives Main topic messages.
     */
    @KafkaListener(topics = "${charges.stream.topic.main}", groupId = "charges.stream.topic.main",
            autoStartup = "${company-metrics.consumer.charges.enable}")
    @Retryable
    public void receive(Message<ResourceChangedData> resourceChangedMessage) {
        logger.info(
                "A new message read from MAIN topic with payload: "
                        + resourceChangedMessage.getPayload());
        metricsProcessor.process(resourceChangedMessage);
    }

    /**
     * Receives Retry topic messages.
     */
    @KafkaListener(topics = "${charges.stream.topic.retry}", groupId = "charges.stream.topic.retry",
            autoStartup = "${company-metrics.consumer.charges.enable}")
    public void retry(Message<ResourceChangedData> resourceChangedMessage) {
        logger.info(
                String.format("A new message read from RETRY topic with payload:%s and headers:%s ",
                        resourceChangedMessage.getPayload(), resourceChangedMessage.getHeaders()));
        metricsProcessor.process(resourceChangedMessage);
    }


}

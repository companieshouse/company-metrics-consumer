package uk.gov.companieshouse.company.metrics.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.processor.CompanyMetricsProcessor;
import uk.gov.companieshouse.delta.ChsDelta;


@Component
public class CompanyMetricsConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyMetricsConsumer.class);

    private final CompanyMetricsProcessor metricsProcessor;

    @Autowired
    public CompanyMetricsConsumer(CompanyMetricsProcessor metricsProcessor) {
        this.metricsProcessor = metricsProcessor;
    }

    /**
     * Receives Main topic messages.
     */
    @KafkaListener(id = "${company.metrics.main-id}",
            topics = "${company.metrics.topic.main}",
            groupId = "${company.metrics.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveMainMessages(Message<ChsDelta> chsDeltaMessage) {
        LOGGER.info("A new message read from MAIN topic with payload: "
                + chsDeltaMessage.getPayload());
        //metricsProcessor.processDelta(chsDeltaMessage);
    }

    /**
     * Receives Retry topic messages.
     */
    @KafkaListener(id = "${company.metrics.retry-id}",
            topics = "${company.metrics.topic.retry}",
            groupId = "${company.metrics.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receiveRetryMessages(Message<ChsDelta> message) {
        LOGGER.info(String.format("A new message read from RETRY topic with payload:%s "
                + "and headers:%s ", message.getPayload(), message.getHeaders()));
        //metricsProcessor.processCompanyMetricsMessage(message);
    }


}

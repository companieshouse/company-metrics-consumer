package uk.gov.companieshouse.company.metrics.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

@Component
public class PscEventStreamConsumer {


    private static final String PSC_DELTA_TYPE = "psc";
    private final MetricsRouter router;
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    public PscEventStreamConsumer(MetricsRouter router) {
        this.router = router;
    }

    /**
     * Receives main topic messages.
     */
    @RetryableTopic(
            attempts = "${company-metrics.consumer.psc-events.stream.retry-attempts}",
            backoff = @Backoff(delayExpression =
                    "${company-metrics.consumer.psc-events.stream.backoff-delay}"),
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${company-metrics.consumer.psc-events.stream.group-id}-retry",
            dltTopicSuffix = "-${company-metrics.consumer.psc-events.stream.group-id}-error",
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(
            id = "${company-metrics.consumer.psc-events.stream.topic}-consumer",
            topics = "#{'${company-metrics.consumer.psc-events.stream.topic}'.split(',')}",
            groupId = "${company-metrics.consumer.psc-events.stream.group-id}",
            autoStartup = "${company-metrics.consumer.psc-events.stream.enable}",
            containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedDataMessage,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        ResourceChangedData payload = resourceChangedDataMessage.getPayload();
        String contextId = payload.getContextId();
        LOGGER.debug(String.format(
                "New message picked up. Topic: %s; Partition: %s; Offset: %s; ContextId: %s",
                topic, partition, offset, contextId));
        try {
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);
            router.route(new ResourceChange(payload), PSC_DELTA_TYPE, updatedBy);
            LOGGER.debug(String.format("Company PSC message processed. ContextId: %s",
                    contextId));
        } catch (Exception exception) {
            LOGGER.error(String.format("Exception processing message. Topic: %s; Offset: %s",
                    topic, offset));
            throw exception;
        }
    }
}
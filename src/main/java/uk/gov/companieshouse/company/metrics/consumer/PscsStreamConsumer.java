package uk.gov.companieshouse.company.metrics.consumer;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class PscsStreamConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private static final String PSCS_DELTA_TYPE = "pscs";
    private final MetricsRouter router;

    public PscsStreamConsumer(MetricsRouter router) {
        this.router = router;
    }

    /**
     * Receives main topic messages.
     */
    @RetryableTopic(
            attempts = "${company-metrics.consumer.psc-statements.stream.retry-attempts}",
            backoff = @Backoff(delayExpression =
                    "${company-metrics.consumer.psc-statements.stream.backoff-delay}"),
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${company-metrics.consumer.psc-statements.stream.group-id}-retry",
            dltTopicSuffix = "-${company-metrics.consumer.psc-statements.stream.group-id}-error",
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(
            id = "${company-metrics.consumer.psc-statements.stream.topic}-consumer",
            topics = "${company-metrics.consumer.psc-statements.stream.topic}",
            groupId = "${company-metrics.consumer.psc-statements.stream.group-id}",
            autoStartup = "${company-metrics.consumer.psc-statements.stream.enable}",
            containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedDataMessage,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        ResourceChangedData payload = resourceChangedDataMessage.getPayload();
        String contextId = payload.getContextId();
        LOGGER.info("Resource changed message received", DataMapHolder.getLogMap());

        try {
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);
            router.route(new ResourceChange(payload), PSCS_DELTA_TYPE, updatedBy);
            LOGGER.debug(String.format("Company PSC message processed. ContextId: %s",
                    contextId));
        } catch (Exception exception) {
            LOGGER.error(String.format("Exception processing message. Topic: %s; Offset: %s",
                    topic, offset));
            throw exception;
        }
    }
}
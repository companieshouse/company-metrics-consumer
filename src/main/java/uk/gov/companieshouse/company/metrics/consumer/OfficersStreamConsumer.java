package uk.gov.companieshouse.company.metrics.consumer;

import static java.lang.String.format;

import java.time.Duration;
import java.time.Instant;

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
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class OfficersStreamConsumer {

    private final MetricsRouter router;
    private final Logger logger;

    public OfficersStreamConsumer(MetricsRouter router, Logger logger) {
        this.router = router;
        this.logger = logger;
    }

    /**
     * Receives main topic messages.
     */
    @RetryableTopic(
            attempts = "${company-metrics.consumer.appointments.stream.retry-attempts}",
            backoff = @Backoff(delayExpression =
                    "${company-metrics.consumer.appointments.stream.backoff-delay}"),
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${company-metrics.consumer.appointments.stream.group-id}-retry",
            dltTopicSuffix = "-${company-metrics.consumer.appointments.stream.group-id}-error",
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(
            id = "${company-metrics.consumer.appointments.stream.topic}-consumer",
            topics = "${company-metrics.consumer.appointments.stream.topic}",
            groupId = "${company-metrics.consumer.appointments.stream.group-id}",
            autoStartup = "${company-metrics.consumer.appointments.stream.enable}",
            containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedDataMessage,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        Instant startTime = Instant.now();
        ResourceChangedData payload = resourceChangedDataMessage.getPayload();
        String contextId = payload.getContextId();
        logger.info(String.format("A new message successfully picked up from topic: %s, "
                        + "partition: %s and offset: %s with contextId: %s",
                topic, partition, offset, contextId));

        try {
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);
            router.route(new ResourceChange(payload), "officers", updatedBy);
            logger.info(format("Company appointments message with contextId: %s is "
                            + "successfully processed in %d milliseconds", contextId,
                    Duration.between(startTime, Instant.now()).toMillis()));
        } catch (Exception exception) {
            logger.errorContext(contextId, format("Exception occurred while processing "
                    + "message on the topic: %s", topic), exception, null);
            throw exception;
        }
    }
}
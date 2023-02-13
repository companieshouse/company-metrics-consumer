package uk.gov.companieshouse.company.metrics.consumer;

import static java.lang.String.format;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.processor.CompanyMetricsProcessor;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Component
public class ChargesStreamConsumer {

    public static final String DELETE_EVENT_TYPE = "deleted";
    private final CompanyMetricsProcessor metricsProcessor;

    private final MetricsRouter chargesRouter;
    private final uk.gov.companieshouse.logging.Logger logger;

    @Autowired
    public ChargesStreamConsumer(CompanyMetricsProcessor metricsProcessor,
                                 MetricsRouter chargesRouter, Logger logger) {
        this.metricsProcessor = metricsProcessor;
        this.chargesRouter = chargesRouter;
        this.logger = logger;
    }

    /**
     * Receives Main topic messages.
     */
    @RetryableTopic(attempts = "${company-metrics.consumer.charges.stream.retry-attempts}",
            backoff = @Backoff(delayExpression = "${company-metrics.consumer.charges.stream.backoff-delay}"),
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${company-metrics.consumer.charges.stream.group-id}-retry",
            dltTopicSuffix = "-${company-metrics.consumer.charges.stream.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(topics = "${company-metrics.consumer.charges.stream.topic}",
            groupId = "${company-metrics.consumer.charges.stream.group-id}",
            autoStartup = "${company-metrics.consumer.charges.stream.enable}",
            containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedMessage,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        Instant startTime = Instant.now();
        ResourceChangedData payload = resourceChangedMessage.getPayload();
        String contextId = payload.getContextId();
        logger.info(String.format("A new message successfully picked up from topic: %s, "
                        + "partition: %s and offset: %s with contextId: %s",
                topic, partition, offset, contextId));

        try {
            final boolean deleteEventType = DELETE_EVENT_TYPE
                    .equalsIgnoreCase(payload.getEvent().getType());
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);

            chargesRouter.route(new ResourceChange(payload), "charges", updatedBy);
            logger.info(format("Charges Metrics Delete message with contextId: %s is "
                            + "successfully processed in %d milliseconds", contextId,
                    Duration.between(startTime, Instant.now()).toMillis()));

            if (deleteEventType) {
                metricsProcessor.processDelete(payload, topic, partition, offset);
                logger.info(format("Charges Metrics Delete message with contextId: %s is "
                                + "successfully processed in %d milliseconds", contextId,
                        Duration.between(startTime, Instant.now()).toMillis()));
            } else {
                metricsProcessor.process(payload, topic, partition, offset);
                logger.info(format("Charges Metrics Delta message with contextId: %s is "
                                + "successfully processed in %d milliseconds", contextId,
                        Duration.between(startTime, Instant.now()).toMillis()));
            }
        } catch (Exception exception) {
            logger.errorContext(contextId, format("Exception occurred while processing "
                    + "message on the topic: %s", topic), exception, null);
            throw exception;
        }
    }

}

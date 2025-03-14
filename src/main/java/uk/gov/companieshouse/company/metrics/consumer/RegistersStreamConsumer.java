package uk.gov.companieshouse.company.metrics.consumer;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.APPLICATION_NAME_SPACE;

import java.time.Duration;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
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
public class RegistersStreamConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final MetricsRouter registersRouter;

    public RegistersStreamConsumer(MetricsRouter registersRouter) {
        this.registersRouter = registersRouter;
    }

    /**
     * Receives Main topic messages.
     */
    @RetryableTopic(attempts = "${company-metrics.consumer.registers.stream.retry-attempts}",
            backoff = @Backoff(delayExpression =
                    "${company-metrics.consumer.registers.stream.backoff-delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${company-metrics.consumer.registers.stream.group-id}-retry",
            dltTopicSuffix = "-${company-metrics.consumer.registers.stream.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(topics = "${company-metrics.consumer.registers.stream.topic}",
            groupId = "${company-metrics.consumer.registers.stream.group-id}",
            autoStartup = "${company-metrics.consumer.registers.stream.enable}",
            containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedMessage,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        Instant startTime = Instant.now();
        ResourceChangedData payload = resourceChangedMessage.getPayload();
        String contextId = payload.getContextId();
        LOGGER.info("Resource changed message received", DataMapHolder.getLogMap());

        try {
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);

            registersRouter.route(new ResourceChange(payload), "registers", updatedBy);
            LOGGER.info(String.format("Registers Metrics message processed in %d milliseconds",
                    Duration.between(startTime, Instant.now()).toMillis()),
                    DataMapHolder.getLogMap());
        } catch (Exception exception) {
            LOGGER.errorContext(contextId, "Exception occurred while processing message",
                    exception, DataMapHolder.getLogMap());
            throw exception;
        }
    }

}

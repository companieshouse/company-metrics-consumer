package uk.gov.companieshouse.company.metrics.consumer;

import static uk.gov.companieshouse.company.metrics.Application.APPLICATION_NAME_SPACE;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ChargesStreamConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final MetricsRouter chargesRouter;

    public ChargesStreamConsumer(final MetricsRouter chargesRouter) {
        this.chargesRouter = chargesRouter;
    }

    @PostConstruct
    public void init() {
        LOGGER.trace("Consumer(class=%s) initialized".formatted(
                this.getClass().getSimpleName()), DataMapHolder.getLogMap());
    }

    /**
     * Receives Main topic messages.
     */
    @RetryableTopic(
            attempts = "${company-metrics.consumer.charges.stream.retry-attempts}",
            backOff = @BackOff(delayString = "${company-metrics.consumer.charges.stream.backoff-delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${company-metrics.consumer.charges.stream.group-id}-retry",
            dltTopicSuffix = "-${company-metrics.consumer.charges.stream.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class
    )
    @KafkaListener(
            id = "${company-metrics.consumer.charges.stream.topic}-consumer",
            topics = "${company-metrics.consumer.charges.stream.topic}",
            groupId = "${company-metrics.consumer.charges.stream.group-id}",
            autoStartup = "${company-metrics.consumer.charges.stream.enable}",
            containerFactory = "listenerContainerFactory"
    )
    public void receive(Message<@NonNull ResourceChangedData> message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        LOGGER.info("receive(topic=%s, partition=%s, kind=%s) method called.".formatted(topic, partition,
                        message.getPayload().getResourceKind()), DataMapHolder.getLogMap());

        Instant startTime = Instant.now();
        ResourceChangedData payload = message.getPayload();
        String contextId = payload.getContextId();

        try {
            ResourceChange resourceChange = new ResourceChange(payload);
            String deltaType = "charges";
            String updatedBy = String.format("%s-%s-%s", topic, partition, offset);

            LOGGER.info("Routing message: (resourceChange=%s, deltaType=%s, updatedBy=%s".formatted(
                    resourceChange, deltaType, updatedBy));

            chargesRouter.route(resourceChange, deltaType, updatedBy);

            long messageProcessingTime = Duration.between(startTime, Instant.now()).toMillis();
            LOGGER.info("Charges Metrics message processed: %d milliseconds".formatted(
                    messageProcessingTime), DataMapHolder.getLogMap());

        } catch (Exception exception) {
            LOGGER.errorContext(contextId, "Exception occurred while processing message",
                    exception, DataMapHolder.getLogMap());
            throw exception;
        }
    }

}

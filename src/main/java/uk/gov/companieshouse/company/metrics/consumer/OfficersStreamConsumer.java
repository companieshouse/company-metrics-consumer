package uk.gov.companieshouse.company.metrics.consumer;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
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
public class OfficersStreamConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private static final String OFFICER_DELTA_TYPE = "officers";
    private final MetricsRouter router;

    public OfficersStreamConsumer(MetricsRouter router) {
        this.router = router;
    }

    /**
     * Receives main topic messages.
     */
    @RetryableTopic(
            attempts = "${company-metrics.consumer.appointments.stream.retry-attempts}",
            backoff = @Backoff(delayExpression =
                    "${company-metrics.consumer.appointments.stream.backoff-delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
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
                        @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        LOGGER.info("Resource changed message received", DataMapHolder.getLogMap());

        ResourceChangedData payload = resourceChangedDataMessage.getPayload();

        try {
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);
            router.route(new ResourceChange(payload), OFFICER_DELTA_TYPE, updatedBy);
            LOGGER.debug("Company appointments message processed", DataMapHolder.getLogMap());
        } catch (Exception exception) {
            LOGGER.error("Exception processing message.", DataMapHolder.getLogMap());
            throw exception;
        }
    }
}
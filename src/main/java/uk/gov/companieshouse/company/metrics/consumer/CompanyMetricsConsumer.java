package uk.gov.companieshouse.company.metrics.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
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
       @RetryableTopic(attempts = "${charges.stream.retry-attempts}",
            backoff = @Backoff(delayExpression = "${charges.stream.backoff-delay}"),
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-${charges.stream.group-id}-retry",
            dltTopicSuffix = "-${charges.stream.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false",
            exclude = NonRetryableException.class)
    @KafkaListener(topics = "${charges.stream.topic}",
                   groupId = "${charges.stream.group-id}",
                   autoStartup = "${company-metrics.consumer.charges.enable}",
                   containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedMessage,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                        @Header(KafkaHeaders.OFFSET) String offset) {
        logger.trace(String.format("A new message from %s topic with payload:%s "
                + "and headers:%s ", topic,
                resourceChangedMessage.getPayload(), resourceChangedMessage.getHeaders()));
        try {
            metricsProcessor.process(resourceChangedMessage, topic, partition, offset);
        } catch (Exception exception) {
            logger.error(String.format("Exception occurred while processing the topic %s "
                    + "with message %s, exception thrown is %s", topic,
                    resourceChangedMessage, exception));
            throw exception;
        }
    }

    /**
     * Receives Retry topic messages.
     */
    @KafkaListener(topics = "${charges.stream.topic.retry}", groupId = "charges.stream.topic.retry",
            autoStartup = "${company-metrics.consumer.charges.enable}")
    public void retry(Message<ResourceChangedData> resourceChangedMessage,
                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                      @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                      @Header(KafkaHeaders.OFFSET) String offset) {
        logger.info(
                String.format("A new message read from RETRY topic with payload:%s and headers:%s ",
                        resourceChangedMessage.getPayload(), resourceChangedMessage.getHeaders()));
        metricsProcessor.process(resourceChangedMessage, topic, partition, offset);
    }



}

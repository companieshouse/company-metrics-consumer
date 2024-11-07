package uk.gov.companieshouse.company.metrics.exception;

import static java.lang.String.format;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_CAUSE_FQCN;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_STACKTRACE;
import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.APPLICATION_NAME_SPACE;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class RetryableTopicErrorInterceptor implements ProducerInterceptor<String, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> message) {
        String nextTopic = message.topic().contains("-error") ? getNextErrorTopic(message)
                : message.topic();
        LOGGER.info(format("Moving message into new topic: %s with value: %s",
                nextTopic, message.value()), DataMapHolder.getLogMap());
        if (nextTopic.contains("-invalid")) {
            return new ProducerRecord<>(nextTopic, message.key(), message.value());
        }

        return message;
    }

    @Override
    public void onAcknowledgement(RecordMetadata recordMetadata, Exception ex) {
        // Not implemented
    }

    @Override
    public void close() {
        // Not implemented
    }

    @Override
    public void configure(Map<String, ?> map) {
        // Not implemented
    }

    private String getNextErrorTopic(ProducerRecord<String, Object> message) {
        Header header1 = message.headers().lastHeader(EXCEPTION_CAUSE_FQCN);
        Header header2 = message.headers().lastHeader(EXCEPTION_STACKTRACE);
        return ((header1 != null
                && new String(header1.value()).contains(NonRetryableErrorException.class.getName()))
                || (header2 != null
                && new String(header2.value()).contains(
                        NonRetryableErrorException.class.getName())))
                ? message.topic().replace("-error", "-invalid") : message.topic();
    }
}

package uk.gov.companieshouse.company.metrics.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.RetryableException;
import uk.gov.companieshouse.company.metrics.producer.CompanyMetricsProducer;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;



@Component
public class CompanyMetricsProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyMetricsProcessor.class);
    private final CompanyMetricsProducer metricsProducer;
    private final CompanyMetricsApiTransformer transformer;

    @Autowired
    public CompanyMetricsProcessor(CompanyMetricsProducer metricsProducer,
                                   CompanyMetricsApiTransformer transformer) {
        this.metricsProducer = metricsProducer;
        this.transformer = transformer;
    }

    /**
     * Process CHS Delta message.
     */
    public void processMetrics(Message<ChsDelta> chsDelta) {
        try {
            MessageHeaders headers = chsDelta.getHeaders();
            final String receivedTopic =
                    Objects.requireNonNull(headers.get(KafkaHeaders.RECEIVED_TOPIC)).toString();
            final ChsDelta payload = chsDelta.getPayload();
            ObjectMapper mapper = new ObjectMapper();

            //FIXME this should be a model from private sdk
            /* CompanyMetrics companyMetrics = mapper.readValue(payload.getData(),
                    CompanyMetrics.class);
            transformer.transform(companyMetrics);*/
        } catch (RetryableException ex) {
            retryMetricsMessage(chsDelta);
        } catch (Exception ex) {
            handleErrorMessage(chsDelta);
            // send to error topic
        }
    }

    public void retryMetricsMessage(Message<ChsDelta> chsDelta) {
    }

    private void handleErrorMessage(Message<ChsDelta> chsDelta) {
    }

}

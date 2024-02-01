package uk.gov.companieshouse.company.metrics.steps;

import io.cucumber.java.en.Given;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.company.metrics.consumer.ResettableCountDownLatch;
import uk.gov.companieshouse.company.metrics.serialization.ResourceChangedDataDeserializer;
import uk.gov.companieshouse.stream.ResourceChangedData;

import static uk.gov.companieshouse.company.metrics.config.CucumberContext.CONTEXT;

public class PscSteps {


    public static final String COMPANY_METRICS_RECALCULATE_POST = "/company/([a-zA-Z0-9]*)/metrics/recalculate";
    public static final String COMPANY_METRICS_RECALCULATE_URI = "/company/%s/metrics/recalculate";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    private static final String EVENT_TYPE = "eventType";

    @Autowired
    private TestSupport testSupport;
    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * The company number extracted from the current avro file
     */
    private String currentCompanyNumber;
    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;
    @Autowired
    private ResettableCountDownLatch resettableCountDownLatch;
    @Autowired
    private ResourceChangedDataDeserializer resourceChangedDataDeserializer;

    private String getRecalculateURI() {
        return CONTEXT.get(COMPANY_METRICS_RECALCULATE_URI) == null
                ? "/company/%s/metrics/recalculate"
                : CONTEXT.get(COMPANY_METRICS_RECALCULATE_URI).toString();
    }

    private String getEventType() {
        return CONTEXT.get(EVENT_TYPE) == null ? "changed" : CONTEXT.get(EVENT_TYPE).toString();
    }

    private void sendKafkaMessage(String topic, ResourceChangedData messageData) {
        kafkaTemplate.send(topic, messageData);
        kafkaTemplate.flush();
    }

    @Given("A resource change data message for {string} with an psc entity exists on the {string} kafka topic")
    public void resourceChangedDataMessageExistsOnMainTopicPsc(String companyNumber, String topic) {

        ResourceChangedData messageData = testSupport.createResourceChangedMessagePsc(
                getRecalculateURI(), companyNumber, getEventType());
        this.currentCompanyNumber = companyNumber;

        sendKafkaMessage(topic, messageData);
    }

}

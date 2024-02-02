package uk.gov.companieshouse.company.metrics.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.consumer.ResettableCountDownLatch;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.serialization.ResourceChangedDataDeserializer;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.company.metrics.config.CucumberContext.CONTEXT;

public class AppointmentSteps {

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

    @Given("A resource change data message for {string} with an appointment entity exists on the {string} kafka topic")
    public void resourceChangedDataMessageExistsOnMainTopic(String companyNumber, String topic) {

        ResourceChangedData messageData = testSupport.createResourceChangedMessageAppointments(
                getRecalculateURI(), companyNumber, getEventType());
        this.currentCompanyNumber = companyNumber;

        sendKafkaMessage(topic, messageData);
    }

    private String getEventType() {
        return CONTEXT.get(EVENT_TYPE) == null ? "changed" : CONTEXT.get(EVENT_TYPE).toString();
    }

    private String getRecalculateURI() {
        return CONTEXT.get(COMPANY_METRICS_RECALCULATE_URI) == null
                ? "/company/%s/metrics/recalculate"
                : CONTEXT.get(COMPANY_METRICS_RECALCULATE_URI).toString();
    }

    @Given("A message for {string} with invalid appointment entity exists on the {string} kafka topic")
    public void resourceChangedDataMessageWithInvalidAppointmentEntityExistsOnTopic(String companyNumber, String topic) {

        ResourceChangedData messageData = testSupport.createResourceChangedMessageInvalidAppointments(
                COMPANY_METRICS_RECALCULATE_URI, companyNumber);
        currentCompanyNumber = companyNumber;

        sendKafkaMessage(topic, messageData);
    }

    @Given("An invalid message exists on the {string} kafka topic")
    public void invalidMessageExistsOnMainKafkaTopic(String topic) {
        kafkaTemplate.send(topic, "invalidMessage");
        kafkaTemplate.flush();
    }

    @Then("The message should be placed on to {string} kafka topic")
    public void messageShouldBePlacedOntoTopic(String topic) {
        ConsumerRecord<String, Object> singleRecord =
                KafkaTestUtils.getSingleRecord(kafkaConsumer,topic, 5000L);

        String recordTopic = singleRecord.topic();
        assertThat(recordTopic).isEqualTo(topic);
        assertThat(singleRecord.value()).isNotNull();
    }

    @Then("A non-retryable exception should be thrown when consuming from {string}")
    public void aNonRetryableExceptionIsThrown(String topic) {
        byte[] invalidMessage = "invalidMessage".getBytes();
        Exception exception = assertThrows(NonRetryableErrorException.class, () ->
            resourceChangedDataDeserializer.deserialize(topic, invalidMessage));

        String expectedMessage = "Malformed data";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @And("The event type is {string}")
    public void eventTypeIsSetToChanged(String eventType){
        CONTEXT.set(EVENT_TYPE, eventType);
    }

    @When("The message is consumed")
    public void messageSuccessfullyConsumed() throws InterruptedException {
        assertThat(resettableCountDownLatch.getCountDownLatch().await(5, TimeUnit.SECONDS)).isTrue();
    }

    @And("A request is sent to the Company Metrics Recalculate endpoint")
    public void requestSentToCompanyMetricsRecalculateEndpoint() {
        List<ServeEvent> serverEvents = checkServeEvents();
        assertMetricsApiSuccessfullyInvoked(serverEvents);
    }

    @Given("The consumer has been configured with api key without internal app privileges for {string}")
    public void consumerBeenConfiguredAsUnauthorised(String companyNumber) {
        stubCompanyMetricsApi(HttpStatus.UNAUTHORIZED.value());
    }

    @Given("The message resource Uri {string} is invalid")
    public void theMessageResourceURIIsInvalid(String invalidResourceUri) {
        CONTEXT.set(COMPANY_METRICS_RECALCULATE_URI, invalidResourceUri);
    }

    @Given("The specified endpoint does not exist within company metrics api")
    public void theSpecifiedEndpointWithinMessageDoesNotExistWithinMetricsConsumer() {
        stubCompanyMetricsApi(HttpStatus.NOT_FOUND.value());
    }

    private void stubCompanyMetricsApi(int statusCode) {
        stubFor(
                post(urlPathMatching(COMPANY_METRICS_RECALCULATE_POST))
                        .willReturn(aResponse()
                                .withStatus(statusCode)
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON))
        );
    }

    private void sendKafkaMessage(String topic, ResourceChangedData messageData) {
        kafkaTemplate.send(topic, messageData);
        kafkaTemplate.flush();
    }

    private void assertMetricsApiSuccessfullyInvoked(List<ServeEvent> serverEvents) {
        ServeEvent serveEvent = getAllServeEvents().get(0);
        assertThat(serveEvent.getRequest().getUrl()).isEqualTo(String.format(COMPANY_METRICS_RECALCULATE_URI, currentCompanyNumber));
        String body = new String(serverEvents.get(0).getRequest().getBody());
        MetricsRecalculateApi payload = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            payload = mapper.readValue(body, MetricsRecalculateApi.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        assertThat(payload).isNotNull();
        assertThat(payload.getMortgage()).isFalse();
        assertThat(payload.getAppointments()).isTrue();
        assertThat(payload.getPersonsWithSignificantControl()).isFalse();
    }

    private List<ServeEvent> checkServeEvents() {
        List<ServeEvent> serverEvents = testSupport.getServeEvents();
        assertThat(serverEvents).hasSize(1);
        assertThat(serverEvents).isNotEmpty(); // assert that the wiremock did something
        return serverEvents;
    }
}

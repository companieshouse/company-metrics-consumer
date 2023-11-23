package uk.gov.companieshouse.company.metrics.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.consumer.ResettableCountDownLatch;
import uk.gov.companieshouse.stream.ResourceChangedData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CompanyMetricsConsumerSteps {

    public static final String COMPANY_METRICS_RECALCULATE_POST = "/company/([a-zA-Z0-9]*)/metrics/recalculate";
    public static final String RETRY_TOPIC_ATTEMPTS = "retry_topic-attempts";
    public static final String COMPANY_METRICS_RECALCULATE_URI = "/company/%s/metrics/recalculate";
    public static final String VALID_COMPANY_CHARGES_URI = "/company/%s/charges/%s";
    private static final String INVALID_COMPANY_CHARGES_URI = "/companyabc//charges";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    private static final String CHARGE_ID = "MYdKM_YnzAmJ8JtSgVXr61n1bgg";

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

    @When("A message for {string} and changed eventType is successfully sent to the Kafka topic {string}")
    public void generateAvroMessageSendToTheKafkaTopic(String companyNumber,
                                                       String topic)
            throws InterruptedException {

        ResourceChangedData avroMessageData = testSupport.createResourceChangedMessageCharges(
                VALID_COMPANY_CHARGES_URI, companyNumber, CHARGE_ID);
        currentCompanyNumber = companyNumber;

        sendKafkaMessage(topic, avroMessageData);
    }

    @And("Company Metrics API returns OK status code")
    public void stubbedCompanyMetricsApiReturnsOKStatusCode(){
        stubCompanyMetricsApi(HttpStatus.OK.value());
    }

    @And("Company Metrics API returns NOT_FOUND status code")
    public void stubbedCompanyMetricsApiReturnsNotFoundStatusCode(){
        stubCompanyMetricsApi(HttpStatus.NOT_FOUND.value());
    }

    @And("Company Metrics API returns BAD_REQUEST status code")
    public void stubbedCompanyMetricsApiReturnsBadRequestStatusCode(){
        stubCompanyMetricsApi(HttpStatus.BAD_REQUEST.value());
    }

    @And("Company Metrics API returns SERVICE_UNAVAILABLE status code")
    public void stubbedCompanyMetricsApiReturnsServiceUnavailableStatusCode(){
        stubCompanyMetricsApi(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Then("The message is successfully consumed and calls company-metrics-api with expected payload")
    public void checkMessageComsumedAndCompanyMetricsApiCalledWithCorrectValues(){
        assertMetricsApiSuccessfullyInvoked(checkServeEvents());
    }

    @Then("The message is successfully consumed and calls charges-data-api")
    public void checkMessageComsumedAndChargesDataApiCalledWithCorrectValues() throws JsonProcessingException {
        assertChargesApiSuccessfullyInvoked(checkServeEvents());
    }

    @When("An invalid message {string} is sent to the Kafka topic {string}")
    public void a_non_avro_message_is_published_and_failed_to_process(String invalidMessageFileName,
                                                                      String topic)
            throws InterruptedException {
        String nonAvroMessageData = testSupport.loadAvroMessageFile(invalidMessageFileName);

        kafkaTemplate.send(topic, nonAvroMessageData);
        kafkaTemplate.flush();

        assertThat(resettableCountDownLatch.getCountDownLatch().await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Then("The message should be moved to topic {string}")
    public void the_message_should_be_moved_to_topic(String topic) {
        ConsumerRecord<String, Object> singleRecord =
                KafkaTestUtils.getSingleRecord(kafkaConsumer, topic, 5000L);
        assertThat(singleRecord.value()).isNotNull();
    }

    @And("Stubbed Company Metrics API should be called {string} times")
    public void stubbed_company_metrics_api_should_be_called_n_times(String times) {
        verify(Integer.parseInt(times), postRequestedFor(
                        urlPathMatching(COMPANY_METRICS_RECALCULATE_POST)
                )
        );
    }

    @Then("The message should be moved to topic {string} after retry attempts of {string}")
    public void the_message_should_retried_and_moved_to_error_topic(String topic, String retryAttempts) {
        ConsumerRecord<String, Object> singleRecord =
                KafkaTestUtils.getSingleRecord(kafkaConsumer, topic, 5000L);

        assertThat(singleRecord.value()).isNotNull();
        List<Header> retryList = StreamSupport.stream(singleRecord.headers().spliterator(), false)
                .filter(header -> header.key().equalsIgnoreCase(RETRY_TOPIC_ATTEMPTS))
                .collect(Collectors.toList());

        assertThat(retryList).hasSize(Integer.parseInt(retryAttempts));
    }

    @When("A message with {string} and deleted eventType for {string} is sent to the Kafka topic {string}")
    public void a_message_with_deleted_event_type_is_sent_to_the_topic(String payload, String companyNumber, String topicName)
            throws InterruptedException {
        ResourceChangedData avroMessageData = testSupport.createResourceDeletedMessageCharges(VALID_COMPANY_CHARGES_URI, companyNumber,
                CHARGE_ID, payload);
        this.currentCompanyNumber = companyNumber;
        sendKafkaMessage(topicName, avroMessageData);
    }

    @When("A message with invalid resourceURI and {string} for {string} is sent to the Kafka topic {string}")
    public void a_message_with_invalidResourceUri_payload_and_delete_eventType_is_sent_to_the_topic(String payload, String companyNumber, String topicName)
            throws InterruptedException {
        ResourceChangedData avroMessageData = testSupport.createResourceDeletedMessageCharges(INVALID_COMPANY_CHARGES_URI, companyNumber,
                CHARGE_ID, payload);
        this.currentCompanyNumber = companyNumber;
        sendKafkaMessage(topicName, avroMessageData);
    }

    @When("A message with invalid resourceURI for {string} is sent to the Kafka topic {string}")
    public void a_message_with_invalidResourceUri_and_delete_eventType_is_sent_to_the_topic(String companyNumber, String topicName)
            throws InterruptedException {
        ResourceChangedData avroMessageData = testSupport.createResourceChangedMessageCharges(
                INVALID_COMPANY_CHARGES_URI, companyNumber, CHARGE_ID);
        this.currentCompanyNumber = companyNumber;
        sendKafkaMessage(topicName, avroMessageData);
    }

    @Then("The message is successfully consumed only once from the {string} topic but failed to process")
    public void the_message_is_successfully_consumed_only_once_from_the_topic_but_failed_to_process(String topicName) {
        Assert.assertThrows(IllegalStateException.class,
                () -> KafkaTestUtils.getSingleRecord(kafkaConsumer, topicName, 5000L));
    }

    @But("Failed to process and immediately moved the message into {string} topic")
    public void immediately_moved_the_message_into_topic(String topicName) {
        Integer retryAttempts = 0;
        moved_the_message_into_topic_after_attempts(topicName, retryAttempts);
    }

    @But("Failed to process and moved the message into {string} topic after {int} attempts")
    public void moved_the_message_into_topic_after_attempts(String topicName, Integer retryAttempts) {
        ConsumerRecord<String, Object> singleRecord =
                KafkaTestUtils.getSingleRecord(kafkaConsumer, topicName, 5000L);

        assertThat(singleRecord.value()).isNotNull();
        List<Header> retryList = StreamSupport.stream(singleRecord.headers().spliterator(), false)
                .filter(header -> header.key().equalsIgnoreCase(RETRY_TOPIC_ATTEMPTS))
                .collect(Collectors.toList());
        assertThat(retryList).hasSize(retryAttempts);
    }

    @Then("Metrics Data API endpoint is never invoked")
    public void metrics_data_api_endpoint_is_never_invoked() {
        verify(0, postRequestedFor(
                        urlPathMatching(COMPANY_METRICS_RECALCULATE_POST)
                )
        );
    }

    @Then("Metrics Data API endpoint is not invoked again")
    public void metrics_data_api_endpoint_is_not_invoked_again() {
        verify(lessThanOrExactly(1), postRequestedFor(
                        urlPathMatching(COMPANY_METRICS_RECALCULATE_POST)
                )
        );
    }

    @NotNull
    private List<ServeEvent> checkServeEvents() {
        List<ServeEvent> serverEvents = testSupport.getServeEvents();
        assertThat(serverEvents).hasSize(1);
        assertThat(serverEvents).isNotEmpty(); // assert that the wiremock did something
        return serverEvents;
    }

    private void assertMetricsApiSuccessfullyInvoked(List<ServeEvent> serverEvents) {
        ServeEvent serveEvent = serverEvents.get(0);
        assertThat(serveEvent.getRequest().getUrl()).isEqualTo(String.format(COMPANY_METRICS_RECALCULATE_URI, currentCompanyNumber));
        String body = new String(serveEvent.getRequest().getBody());
        MetricsRecalculateApi payload = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            payload = mapper.readValue(body, MetricsRecalculateApi.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        assertThat(payload).isNotNull();
        assertThat(payload.getMortgage()).isTrue();
        assertThat(payload.getAppointments()).isFalse();
        assertThat(payload.getPersonsWithSignificantControl()).isFalse();
    }

    private void assertChargesApiSuccessfullyInvoked(List<ServeEvent> serverEvents) throws JsonProcessingException {
        ServeEvent serveEvent = serverEvents.get(1);

        assertThat(serveEvent.getRequest().getUrl()).isEqualTo(String.format(VALID_COMPANY_CHARGES_URI, currentCompanyNumber, CHARGE_ID));

        ChargeApi payload = getPayloadFromWireMock(serveEvent);
        assertThat(payload).isNotNull();
        assertThat(payload.getId()).isEqualTo("3001283055");
        assertThat(payload.getChargeNumber()).isEqualTo(50);
        assertThat(payload.getChargeCode()).isEqualTo("081242070049");
    }

    @Nullable
    private ChargeApi getPayloadFromWireMock(ServeEvent serveEvent) throws JsonProcessingException {
        String body = new String(serveEvent.getResponse().getBody());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(body, ChargeApi.class);
    }

    private void sendKafkaMessage(String topic, ResourceChangedData avroMessageData) throws InterruptedException {
        kafkaTemplate.send(topic, avroMessageData);
        kafkaTemplate.flush();
        assertThat(resettableCountDownLatch.getCountDownLatch().await(5, TimeUnit.SECONDS)).isTrue();
    }

    private void stubCompanyMetricsApi(int statusCode) {
        stubFor(
                post(urlPathMatching(COMPANY_METRICS_RECALCULATE_POST))
                        .willReturn(aResponse()
                                .withStatus(statusCode)
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON))
        );
    }

}

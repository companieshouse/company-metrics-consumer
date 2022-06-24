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
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class CompanyMetricsConsumerSteps {

    private static final String HEALTHCHECK_URI = "/company-metrics-consumer/healthcheck";
    private static final String HEALTHCHECK_RESPONSE_BODY = "{\"status\":\"UP\"}";
    public static final String COMPANY_METRICS_RECALCULATE_POST = "/company/([a-zA-Z0-9]*)/metrics/recalculate";
    public static final String RETRY_TOPIC_ATTEMPTS = "retry_topic-attempts";
    public static final String COMPANY_METRICS_RECALCULATE_URI = "/company/%s/metrics/recalculate";
    public static final String VALID_COMPANY_CHARGES_PATH = "/company/%s/charges/%s";

    @Autowired
    private TestSupport testSupport;

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected TestRestTemplate restTemplate;

    private WireMockServer wireMockServer;

    /**
     * The company number extracted from the current avro file
     */
    private String currentCompanyNumber;

    private String currentChargeId;

    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;

    @Given("Company Metrics Consumer component is running and Company Metrics API is stubbed")
    public void theCompanyMetricsIsRunning() {
        ResponseEntity<String> response = restTemplate.getForEntity(HEALTHCHECK_URI, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(200));
        assertThat(response.getBody()).isEqualTo(HEALTHCHECK_RESPONSE_BODY);
        wireMockServer = testSupport.setupWiremock();
        assertThat(wireMockServer.isRunning()).isTrue();
    }

    @And("Charges Data API endpoint is stubbed for {string} and {string} and will return {string} http response code")
    public void stubChargesDataApiEndpointForResponse(String companyNumber,
                                                      String chargeId,
                                                      String statusValue){
        int requiredStatusValue = Integer.parseInt(statusValue);
        this.currentCompanyNumber = companyNumber;
        this.currentChargeId = chargeId;

        stubChargesDataApiGetEndpoint(requiredStatusValue, "stubChargesDataApiEndpointForResponse");
    }

    @When("A valid avro message for {string} and {string} and {string} is generated and sent to the Kafka topic {string} and stubbed Company Metrics API returns {string}")
    public void generateAvroMessageSendToTheKafkaTopic(String companyNumber,
                                                       String companyLinksFormat,
                                                       String chargeId,
                                                       String topic,
                                                       String statusCode)
            throws InterruptedException, IOException {
        ResourceChangedData avroMessageData = testSupport.createResourceChangedMessage(
                companyLinksFormat, companyNumber, chargeId);
        currentCompanyNumber = companyNumber;
        currentChargeId = chargeId;
        stubCompanyMetricsApi(currentCompanyNumber,
                "an_avro_message_is_published_to_topic",
                Integer.parseInt(statusCode));
        sendKafkaMessage(topic, avroMessageData);
    }

    @Then("The message is successfully consumed and company number is successfully extracted to call company-metrics-api POST endpoint with expected payload")
    public void checkMessageComsumedAndCompanyMetricsApiCalledWithCorrectValues(){
        List<ServeEvent> serverEvents = checkServeEvents();
        assertMetricsApiSuccessfullyInvoked(serverEvents);
    }

    @NotNull
    private List<ServeEvent> checkServeEvents() {
        List<ServeEvent> serverEvents = testSupport.getServeEvents();
        assertThat(serverEvents.size()).isEqualTo(2);
        assertThat(serverEvents.isEmpty()).isFalse(); // assert that the wiremock did something
        return serverEvents;
    }

    @Then("The message is successfully consumed and company number is successfully extracted to call charges-data-api GET endpoint")
    public void checkMessageComsumedAndChargesDataApiCalledWithCorrectValues() throws JsonProcessingException {
        List<ServeEvent> serverEvents = checkServeEvents();
        assertChargesApiSuccessfullyInvoked(serverEvents);
    }

    private void assertMetricsApiSuccessfullyInvoked(List<ServeEvent> serverEvents) {
        ServeEvent serveEvent = getServeEvent(serverEvents, "an_avro_message_is_published_to_topic");
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
        assertThat(payload.getMortgage()).isTrue();
        assertThat(payload.getAppointments()).isFalse();
        assertThat(payload.getPersonsWithSignificantControl()).isFalse();
    }

    private void assertChargesApiSuccessfullyInvoked(List<ServeEvent> serverEvents) throws JsonProcessingException {
        ServeEvent serveEvent = getServeEvent(serverEvents, "stubChargesDataApiEndpointForResponse");

        assertThat(serveEvent.getRequest().getUrl()).isEqualTo(String.format(VALID_COMPANY_CHARGES_PATH, currentCompanyNumber, currentChargeId));

        ChargeApi payload = getPayloadFromWireMock(serveEvent, ChargeApi.class);
        assertThat(payload).isNotNull();
        assertThat(payload.getId()).isEqualTo("3001283055");
        assertThat(payload.getChargeNumber()).isEqualTo(50);
        assertThat(payload.getChargeCode()).isEqualTo("081242070049");
    }

    @Nullable
    private <T> T getPayloadFromWireMock(ServeEvent serveEvent, Class<T> T) throws JsonProcessingException {
        String body = new String(serveEvent.getResponse().getBody());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(body, T);
    }

    @When("A non-avro message {string} is sent to the Kafka topic {string} and stubbed Company Metrics API returns {string}")
    public void a_non_avro_message_is_published_and_failed_to_process(String nonAvroMessageFileName,
                                                                      String topic,
                                                                      String statusCode)
            throws InterruptedException {
        String nonAvroMessageData = testSupport.loadAvroMessageFile(nonAvroMessageFileName);
        stubCompanyMetricsApi(currentCompanyNumber, "a_non_avro_message_is_published_and_failed_to_process",
                Integer.parseInt(statusCode));

        kafkaTemplate.send(topic, nonAvroMessageData);
        kafkaTemplate.flush();

        TimeUnit.SECONDS.sleep(1);
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

        assertThat(retryList.size()).isEqualTo(Integer.parseInt(retryAttempts));
    }

    private void stubCompanyMetricsApi(String companyNumber, String testMethodIdentifier, int statusCode) {
         stubFor(
            post(urlPathMatching(COMPANY_METRICS_RECALCULATE_POST))
                .willReturn(aResponse()
                    .withStatus(statusCode)
                    .withHeader("Content-Type", "application/json"))
                .withMetadata(metadata()
                    .list("tags", testMethodIdentifier))
        );
    }

    @Given("Company Metrics Consumer component is successfully running")
    public void company_metrics_consumer_component_is_successfully_running() {
      this.theCompanyMetricsIsRunning();
    }

    @Given("Stubbed Company Metrics API endpoint will return {int} http response code")
    public void stubbed_company_metrics_api_endpoint_will_return_http_response_code(Integer responseCode) {
        stubCompanyMetricsApi(currentCompanyNumber,
                "an_avro_message_is_published_to_topic",
                responseCode);
    }

    @When("A valid avro message {string} with deleted event for {string} and {string} and {string} is sent to the Kafka topic {string}")
    public void a_valid_avro_message_with_deleted_event_for_and_is_sent_to_the_kafka_topic(String payload, String companyNumber, String resourceUri,
                                                                                           String chargedId, String topicName)
            throws InterruptedException {
        ResourceChangedData avroMessageData = testSupport.createResourceDeletedMessage(resourceUri, companyNumber,
                chargedId, payload);
        this.currentCompanyNumber = companyNumber;
        this.currentChargeId = chargedId;
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
        assertThat(retryList.size()).isEqualTo(retryAttempts);
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

    private void sendKafkaMessage(String topic, ResourceChangedData avroMessageData) throws InterruptedException {
        kafkaTemplate.send(topic, avroMessageData);
        kafkaTemplate.flush();
        TimeUnit.SECONDS.sleep(1);
    }

    private ServeEvent getServeEvent(List<ServeEvent> serverEvents, String tag) {
        ServeEvent serveEvent = serverEvents.stream()
                .filter(event -> event.getStubMapping().getMetadata().getList("tags").get(0)
                        .toString().equalsIgnoreCase(tag))
                .collect(Collectors.toList()).get(0);
        return serveEvent;
    }

    private void stubChargesDataApiGetEndpoint(int requiredStatusValue, String testIdentifier) {

        String chargesRecord = testSupport.loadFile("payloads", "get_charge_response.json");

        stubFor(
                get(urlEqualTo(String.format(VALID_COMPANY_CHARGES_PATH, currentCompanyNumber, currentChargeId)))
                        .willReturn(aResponse()
                                .withStatus(requiredStatusValue)
                                .withHeader("Content-Type", "application/json")
                                .withBody(chargesRecord))
                        .withMetadata(metadata()
                                .list("tags", testIdentifier))
        );
    }

}

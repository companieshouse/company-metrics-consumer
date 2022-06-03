package uk.gov.companieshouse.company.metrics.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class CompanyMetricsConsumerSteps {

    public static final String COMPANY_METRICS_RECALCULATE_POST = "/company/([a-zA-Z0-9]*)/metrics/recalculate";
    public static final String RETRY_TOPIC_ATTEMPTS = "retry_topic-attempts";
    public static final String COMPANY_METRICS_RECALCULATE_URI = "/company/%s/metrics/recalculate";
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

    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;

    @Given("Company Metrics Consumer component is running and Company Metrics API is stubbed")
    public void theCompanyMetricsIsRunning() {
        ResponseEntity<String> response = restTemplate.getForEntity("/healthcheck", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(200));
        assertThat(response.getBody()).isEqualTo("I am healthy");
        wireMockServer = testSupport.setupWiremock();
        assertThat(wireMockServer.isRunning()).isTrue();
    }

    @When("A valid avro message for {string} and {string} is generated and sent to the Kafka topic {string} and stubbed Company Metrics API returns {string}")
    public void generateAvroMessageSendToTheKafkaTopic(String companyNumber,
                                                       String companyLinksFormat,
                                                       String topic,
                                                       String statusCode)
            throws InterruptedException, IOException {
        ResourceChangedData avroMessageData = testSupport.createResourceChangedMessage(
                companyLinksFormat, companyNumber);
        currentCompanyNumber = companyNumber;
        stubCompanyMetricsApi(currentCompanyNumber,
                "an_avro_message_is_published_to_topic",
                Integer.parseInt(statusCode));
        kafkaTemplate.send(topic, avroMessageData);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    @Then("The message is successfully consumed and company number is susccessfully extracted to call company-metrics-api POST endpoint with expected payload")
    public void checkMessageComsumedAndCompanyMetricsApiCalledWithCorrectValues(){
        List<ServeEvent> serverEvents = testSupport.getServeEvents();
        assertThat(serverEvents.size()).isEqualTo(1);
        assertThat(serverEvents.isEmpty()).isFalse(); // assert that the wiremock did something
        assertThat(serverEvents.get(0).getRequest().getUrl()).isEqualTo(String.format(COMPANY_METRICS_RECALCULATE_URI, currentCompanyNumber));
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

    @When("A non-avro message {string} is sent to the Kafka topic {string} and stubbed Company Metrics API returns {string}")
    public void a_non_avro_message_is_published_and_failed_to_process(String nonAvroMessageFileName,
                                                                      String topic,
                                                                      String statusCode)
            throws InterruptedException {
        String nonAvroMessageData = testSupport.loadAvroMessageFile(nonAvroMessageFileName);
        stubCompanyMetricsApi(currentCompanyNumber, "a_non_avro_message_is_published_and_failed_to_process",
                Integer.parseInt(statusCode));
        kafkaTemplate.send(topic, nonAvroMessageData);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    @Then("The message should be moved to topic {string}")
    public void the_message_should_be_moved_to_topic(String topic) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer, topic);
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
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer, topic);

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

    @Then("The message is successfully consumed and company number is successfully extracted to call company-metrics-api recalculate POST endpoint with expected payload")
    public void the_message_is_successfully_consumed_and_company_number_is_successfully_extracted_to_call_company_metrics_api_recalculate_post_endpoint_with_expected_payload() {
        List<ServeEvent> serverEvents = testSupport.getServeEvents();
        assertThat(serverEvents.size()).isEqualTo(1);
        assertThat(serverEvents.isEmpty()).isFalse(); // assert that the wiremock did something
        assertThat(serverEvents.get(0).getRequest().getUrl()).isEqualTo(String.format(COMPANY_METRICS_RECALCULATE_URI, currentCompanyNumber));
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

    @When("A valid avro message {string} with deleted event for {string} and {string} is sent to the Kafka topic {string}")
    public void a_valid_avro_message_with_deleted_event_for_and_is_sent_to_the_kafka_topic(String payload, String companyNumber, String resourceUri, String topicName)
            throws InterruptedException {
        ResourceChangedData avroMessageData = testSupport.createResourceDeletedMessage(resourceUri, companyNumber, payload);
        this.currentCompanyNumber = companyNumber;
        kafkaTemplate.send(topicName, avroMessageData);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    @Then("The message is successfully consumed only once from the {string} topic but failed to process")
    public void the_message_is_successfully_consumed_only_once_from_the_topic_but_failed_to_process(String topicName) {
        Assert.assertThrows(IllegalStateException.class, () -> KafkaTestUtils.getSingleRecord(kafkaConsumer, topicName));
    }

    @But("Failed to process and immediately moved the message into {string} topic")
    public void immediately_moved_the_message_into_topic(String topicName) {
        Integer retryAttempts = 0;
        moved_the_message_into_topic_after_attempts(topicName, retryAttempts);
    }

    @But("Failed to process and moved the message into {string} topic after {int} attempts")
    public void moved_the_message_into_topic_after_attempts(String topicName, Integer retryAttempts) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer, topicName);

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


}

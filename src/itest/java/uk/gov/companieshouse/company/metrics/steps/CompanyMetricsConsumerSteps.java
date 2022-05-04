package uk.gov.companieshouse.company.metrics.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class CompanyMetricsConsumerSteps {

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
        assertThat(serverEvents.get(0).getRequest().getUrl()).isEqualTo(String.format("/company/%s/metrics/recalculate", currentCompanyNumber));
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
                        urlPathMatching("/company/([a-zA-Z0-9]*)/metrics/recalculate")
                )
        );
    }

    @Then("The message should be moved to topic {string} after retry attempts of {string}")
    public void the_message_should_retried_and_moved_to_error_topic(String topic, String retryAttempts) {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(kafkaConsumer, topic);

        assertThat(singleRecord.value()).isNotNull();
        List<Header> retryList = StreamSupport.stream(singleRecord.headers().spliterator(), false)
                .filter(header -> header.key().equalsIgnoreCase("retry_topic-attempts"))
                .collect(Collectors.toList());

        assertThat(retryList.size()).isEqualTo(Integer.parseInt(retryAttempts));
    }

    private void stubCompanyMetricsApi(String companyNumber, String testMethodIdentifier, int statusCode) {
         stubFor(
            post(urlPathMatching("/company/([a-zA-Z0-9]*)/metrics/recalculate"))
                .willReturn(aResponse()
                    .withStatus(statusCode)
                    .withHeader("Content-Type", "application/json"))
                .withMetadata(metadata()
                    .list("tags", testMethodIdentifier))
        );
    }

}

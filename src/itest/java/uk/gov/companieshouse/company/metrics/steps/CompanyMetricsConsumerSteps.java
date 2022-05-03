package uk.gov.companieshouse.company.metrics.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
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

    @Given("Company Metrics Consumer component is running and Charges Data API is stubbed")
    public void theCompanyMetricsIsRunning() {
        ResponseEntity<String> response = restTemplate.getForEntity("/healthcheck", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(200));
        assertThat(response.getBody()).isEqualTo("I am healthy");
        wireMockServer = testSupport.setupWiremock();
        assertThat(wireMockServer.isRunning()).isTrue();
    }

    @When("A valid avro message for {string} is generated and sent to the Kafka topic {string}")
    public void generateAvroMessageSendToTheKafkaTopic(String companyNumber, String topic)
        throws InterruptedException {
        ResourceChangedData avroMessageData = testSupport.createResourceChangedMessage(companyNumber);
        currentCompanyNumber = companyNumber;
        stubCompanyMetricsApi(currentCompanyNumber, "an_avro_message_is_published_to_topic");
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

    private void stubCompanyMetricsApi(String companyNumber, String testMethodIdentifier) {
         stubFor(
            post(urlPathMatching("/company/([a-zA-Z0-9]*)/metrics/recalculate"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json"))
                .withMetadata(metadata()
                    .list("tags", testMethodIdentifier))
        );
    }

}

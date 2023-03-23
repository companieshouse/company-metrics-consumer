package uk.gov.companieshouse.company.metrics.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.company.metrics.consumer.ResettableCountDownLatch;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.company.metrics.config.CucumberContext.CONTEXT;

public class CommonApiSteps {

    private static final String HEALTHCHECK_URI = "/company-metrics-consumer/healthcheck";
    private static final String HEALTHCHECK_RESPONSE_BODY = "{\"status\":\"UP\"}";
    private ResponseEntity<String> lastResponse;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;
    @Autowired
    private ResettableCountDownLatch resettableCountDownLatch;

    @Autowired
    private TestSupport testSupport;

    /*
    Below setup method is where anything that needs to be executed at the start of cucumber integration tests needs to go.
    In order to avoid a race condition there needs to be only one of these setup methods as Cucumber will look for any method
    annotated with @Before.
     */
    @Before
    public void setup() {
        resettableCountDownLatch.resetLatch(4);
        ResponseEntity<String> response = restTemplate.getForEntity(HEALTHCHECK_URI, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(200));
        assertThat(response.getBody()).isEqualTo(HEALTHCHECK_RESPONSE_BODY);
        kafkaConsumer.poll(Duration.ofSeconds(1));
        configureWiremock();
        CONTEXT.clear();
    }

    private void configureWiremock() {
        WireMockServer wireMockServer = testSupport.setupWiremock();
        assertThat(wireMockServer.isRunning()).isTrue();
    }

    @Given("the application running")
    public void theApplicationRunning() {
        assertThat(restTemplate).isNotNull();
        lastResponse = null;
    }

    @When("the client invokes {string} endpoint")
    public void theClientInvokesAnEndpoint(String url) {
        lastResponse = restTemplate.getForEntity(url, String.class);
    }

    @Then("the client receives status code of {int}")
    public void theClientReceivesStatusCodeOf(int code) {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.valueOf(code));
    }

    @And("the client receives response body as {string}")
    public void theClientReceivesRawResponse(String response) {
        assertThat(lastResponse.getBody()).isEqualTo(response);
    }

}

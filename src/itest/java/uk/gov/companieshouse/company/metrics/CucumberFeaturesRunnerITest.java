package uk.gov.companieshouse.company.metrics;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/itest/resources/features",
        plugin = {"pretty", "json:target/cucumber-report.json"},
        glue = {"uk.gov.companieshouse.company.metrics"})
@CucumberContextConfiguration
@AutoConfigureTestRestTemplate
public class CucumberFeaturesRunnerITest extends AbstractIntegrationTest {

}

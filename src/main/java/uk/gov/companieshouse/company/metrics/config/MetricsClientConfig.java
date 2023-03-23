package uk.gov.companieshouse.company.metrics.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.company.metrics.service.MetricsClient;

@Configuration
public class MetricsClientConfig {
    @Bean
    Map<String, Map<String, MetricsClient>> metricsClientMap(MetricsClient changedChargesClient,
                                                             MetricsClient deletedChargesClient,
                                                             MetricsClient appointmentsClient,
                                                             MetricsClient pscStatementsClient) {
        return Map.of(
                "charges", Map.of(
                        "changed", changedChargesClient,
                        "deleted", deletedChargesClient),
                "officers", Map.of(
                        "changed", appointmentsClient,
                        "deleted", appointmentsClient),
                "psc-statements", Map.of(
                        "changed", pscStatementsClient,
                        "deleted", pscStatementsClient));
    }
}

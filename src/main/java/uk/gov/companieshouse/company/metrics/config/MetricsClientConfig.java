package uk.gov.companieshouse.company.metrics.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.company.metrics.service.MetricsClient;

@Configuration
public class MetricsClientConfig {

    private static final String CHARGES = "charges";
    private static final String CHANGED = "changed";
    private static final String DELETED = "deleted";
    private static final String OFFICERS = "officers";
    private static final String PSCS = "pscs";
    private static final String REGISTERS = "registers";

    @Bean
    Map<String, Map<String, MetricsClient>> metricsClientMap(MetricsClient chargesClient,
                                                             MetricsClient appointmentsClient,
                                                             MetricsClient pscsClient,
                                                             MetricsClient registersClient) {
        return Map.of(
                CHARGES, Map.of(
                        CHANGED, chargesClient,
                        DELETED, chargesClient),
                OFFICERS, Map.of(
                        CHANGED, appointmentsClient,
                        DELETED, appointmentsClient),
                PSCS, Map.of(
                        CHANGED, pscsClient,
                        DELETED, pscsClient),
                REGISTERS, Map.of(
                        CHANGED, registersClient,
                        DELETED, registersClient));
    }
}

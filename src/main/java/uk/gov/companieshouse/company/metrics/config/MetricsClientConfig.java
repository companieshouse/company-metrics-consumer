package uk.gov.companieshouse.company.metrics.config;

import avro.shaded.com.google.common.collect.ImmutableMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.company.metrics.service.MetricsClient;


@Configuration
public class MetricsClientConfig {

    @Bean
    Map<String, Map<String, MetricsClient>> metricsClientMap(MetricsClient changedChargesClient,
                                                             MetricsClient deletedChargesClient) {
        return ImmutableMap.of(
                "charges", ImmutableMap.of(
                        "changed", changedChargesClient,
                        "deleted", deletedChargesClient
                )
        );

    }
}

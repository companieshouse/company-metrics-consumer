package uk.gov.companieshouse.company.metrics.config;

import avro.shaded.com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.company.metrics.service.MetricsClient;

import java.util.Map;

@Configuration
public class MetricsClientConfig {

    @Bean
    Map<String, Map<String, MetricsClient>> metricsClientMap(MetricsClient chargesClient) {
        return ImmutableMap.of(
                "charges", ImmutableMap.of(
                        "changed", chargesClient
                )
        );

    }
}

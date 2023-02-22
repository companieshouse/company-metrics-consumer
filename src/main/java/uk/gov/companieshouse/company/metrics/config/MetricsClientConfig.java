package uk.gov.companieshouse.company.metrics.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.company.metrics.service.MetricsClient;


@Configuration
public class MetricsClientConfig {

    @Autowired
    private MetricsClient changedChargesClient;
    @Autowired
    private MetricsClient deletedChargesClient;
    @Autowired
    private MetricsClient appointmentsClient;

    @Bean
    Map<String, Map<String, MetricsClient>> metricsClientMap() {
        Map<String, Map<String, MetricsClient>> metricsClientConfig = new HashMap<>();

        Map<String, MetricsClient> chargesClientConfig = new HashMap<>();
        chargesClientConfig.put("changed", changedChargesClient);
        chargesClientConfig.put("deleted", deletedChargesClient);
        metricsClientConfig.put("charges", chargesClientConfig);

        Map<String, MetricsClient> appointmentsClientConfig = new HashMap<>();
        appointmentsClientConfig.put("changed", appointmentsClient);
        appointmentsClientConfig.put("deleted", appointmentsClient);
        metricsClientConfig.put("officers", appointmentsClientConfig);

        return metricsClientConfig;
    }
}

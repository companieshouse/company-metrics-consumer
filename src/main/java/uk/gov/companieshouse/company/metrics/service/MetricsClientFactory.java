package uk.gov.companieshouse.company.metrics.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;


@Component
public class MetricsClientFactory {


    private final Map<String, Map<String, MetricsClient>> metricsClientMap;
    private final NullMetricsClient nullMetricsClient;

    public MetricsClientFactory(Map<String, Map<String, MetricsClient>> metricsClientMap,
                             NullMetricsClient nullMetricsClient) {
        this.metricsClientMap = metricsClientMap;
        this.nullMetricsClient = nullMetricsClient;
    }

    public MetricsClient getMetricsClient(String deltaType, String eventType) {
        return metricsClientMap.getOrDefault(deltaType, Collections.emptyMap())
                .getOrDefault(eventType, nullMetricsClient);
    }
    
}

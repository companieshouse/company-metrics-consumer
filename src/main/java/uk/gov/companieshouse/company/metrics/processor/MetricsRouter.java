package uk.gov.companieshouse.company.metrics.processor;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.service.MetricsClientFactory;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;

public class MetricsRouter implements MetricsRoutable {

    private MetricsClientFactory factory;

    public MetricsRouter(MetricsClientFactory factory) {
        this.factory = factory;
    }

    @Override
    public void route(ResourceChange message, String deltaType) {

    }
}

package uk.gov.companieshouse.company.metrics.processor;

import uk.gov.companieshouse.company.metrics.type.ResourceChange;

public interface MetricsRoutable {

    void route(ResourceChange message, String deltaType);
}

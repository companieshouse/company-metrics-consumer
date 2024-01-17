package uk.gov.companieshouse.company.metrics.processor;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.service.CompanyNumberExtractable;
import uk.gov.companieshouse.company.metrics.service.MetricsClientFactory;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;

@Component
public class MetricsRouter implements MetricsRoutable {

    private final CompanyNumberExtractable extractor;
    private final MetricsClientFactory factory;

    public MetricsRouter(CompanyNumberExtractable extractor, MetricsClientFactory factory) {
        this.extractor = extractor;
        this.factory = factory;
    }

    @Override
    public void route(ResourceChange message, String deltaType, String updatedBy) {
        String eventType = message.getData().getEvent().getType();
        String resourceUri = message.getData().getResourceUri();
        String contextId = message.getData().getContextId();
        String companyNumber = extractor.extractCompanyNumber(message.getData().getResourceUri());
        factory.getMetricsClient(deltaType, eventType)
                .postMetrics(companyNumber, updatedBy, resourceUri, contextId);
    }
}

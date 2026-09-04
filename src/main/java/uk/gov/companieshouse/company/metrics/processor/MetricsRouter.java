package uk.gov.companieshouse.company.metrics.processor;

import static uk.gov.companieshouse.company.metrics.Application.APPLICATION_NAME_SPACE;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.company.metrics.service.CompanyNumberExtractable;
import uk.gov.companieshouse.company.metrics.service.MetricsClientFactory;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class MetricsRouter implements MetricsRoutable {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final CompanyNumberExtractable extractor;
    private final MetricsClientFactory factory;

    public MetricsRouter(CompanyNumberExtractable extractor, MetricsClientFactory factory) {
        this.extractor = extractor;
        this.factory = factory;
    }

    @Override
    public void route(ResourceChange message, String deltaType, String updatedBy) {
        LOGGER.info("route(message=%s) method called".formatted(message.toString()), DataMapHolder.getLogMap());

        String eventType = message.getData().getEvent().getType();
        String resourceUri = message.getData().getResourceUri();
        String companyNumber = extractor.extractCompanyNumber(message.getData().getResourceUri());

        DataMapHolder.get().companyNumber(companyNumber);

        factory.getMetricsClient(deltaType, eventType)
                .postMetrics(companyNumber, updatedBy, resourceUri);
    }
}

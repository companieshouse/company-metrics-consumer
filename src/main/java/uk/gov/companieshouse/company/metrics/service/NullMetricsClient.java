package uk.gov.companieshouse.company.metrics.service;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class NullMetricsClient implements MetricsClient {

    private final Logger logger;

    public NullMetricsClient(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void postMetrics(String companyNumber,
                            String updatedBy,
                            String resourceUri,
                            String contextId) {
        logger.error(String.format(
                "Invalid delta type and/or event type for company number %s", companyNumber));
        throw new NonRetryableErrorException("Invalid delta type and/or event type");
    }
}

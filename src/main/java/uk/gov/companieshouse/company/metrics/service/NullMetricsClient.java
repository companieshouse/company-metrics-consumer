package uk.gov.companieshouse.company.metrics.service;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class NullMetricsClient implements MetricsClient {

    private final Logger logger;
    private static final String INVALID_DELTA_EVENT_TYPE = "Invalid delta type and/or event type for company number: ";

    public NullMetricsClient(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void postMetrics(String companyNumber,
                            String updatedBy,
                            String resourceUri,
                            String contextId) {
        logger.error(String.format(INVALID_DELTA_EVENT_TYPE +
                "%s", companyNumber));
        throw new NonRetryableErrorException(String.format( INVALID_DELTA_EVENT_TYPE + "%s", companyNumber));
    }
}

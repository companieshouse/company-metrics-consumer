package uk.gov.companieshouse.company.metrics.service;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class NullMetricsClient implements MetricsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private static final String INVALID_DELTA_EVENT_TYPE =
            "Invalid delta type and/or event type for company number %s";

    @Override
    public void postMetrics(String companyNumber,
                            String updatedBy,
                            String resourceUri) {
        LOGGER.error(String.format(INVALID_DELTA_EVENT_TYPE, companyNumber),  DataMapHolder.getLogMap());
        throw new NonRetryableErrorException(
                String.format(INVALID_DELTA_EVENT_TYPE, companyNumber));
    }
}

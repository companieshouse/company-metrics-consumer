package uk.gov.companieshouse.company.metrics.service;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class MetricsApiResponseHandler implements ResponseHandler {

    private static final String FAILED_MSG = "Failed recalculating %s for company %s";
    private static final String ERROR_MSG = "Error %s recalculating %s for company %s";

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    /**
     * Handles logging an error message and throwing the appropriate exception when a
     * URIValidationException is caught in the client.
     *
     * @param companyNumber The company number for the delta that has come through on the topic.
     * @param deltaType The type of delta that has come through on the topic.
     * @param ex The exception that was caught in the client.
     */
    @Override
    public void handle(String companyNumber, String deltaType, URIValidationException ex) {
        String message = String.format(FAILED_MSG, deltaType, companyNumber);
        LOGGER.error(message, DataMapHolder.getLogMap());
        throw new NonRetryableErrorException(message, ex);
    }

    /**
     * Handles logging an error message and throwing the appropriate exception when an
     * IllegalArgumentException is caught in the client.
     *
     * @param companyNumber The company number for the delta that has come through on the topic.
     * @param deltaType The type of delta that has come through on the topic.
     * @param ex The exception that was caught in the client.
     */
    @Override
    public void handle(String companyNumber, String deltaType, IllegalArgumentException ex) {
        String message = String.format(FAILED_MSG, deltaType, companyNumber);
        String causeMessage = ex.getCause() != null
                ? String.format("; %s", ex.getCause().getMessage()) : "";
        LOGGER.info(message + causeMessage, DataMapHolder.getLogMap());
        throw new RetryableErrorException(message, ex);
    }

    /**
     * Handles logging an error message and throwing the appropriate exception
     * when an ApiErrorResponseException is caught in the client.
     *
     * @param companyNumber The company number for the delta that has come through on the topic.
     * @param deltaType The type of delta that has come through on the topic.
     * @param ex The exception that was caught in the client.
     */
    @Override
    public void handle(String companyNumber, String deltaType, ApiErrorResponseException ex) {
        String message = String.format(ERROR_MSG, ex.getStatusCode(), deltaType, companyNumber);
        Map<String, Object> logMap = DataMapHolder.getLogMap();
        logMap.put("status", ex.getStatusCode());
        if (HttpStatus.valueOf(ex.getStatusCode()).is5xxServerError()) {
            LOGGER.info(message, logMap);
            throw new RetryableErrorException(message, ex);
        } else {
            LOGGER.error(message, logMap);
            throw new NonRetryableErrorException(message, ex);
        }
    }
}

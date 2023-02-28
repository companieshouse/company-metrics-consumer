package uk.gov.companieshouse.company.metrics.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class MetricsApiResponseHandlerImpl implements MetricsApiResponseHandler {

    public static final String FAILED_MSG = "Failed recalculating %s for company %s";
    public static final String ERROR_MSG = "Error %s recalculating %s for company %s";

    private final Logger logger;

    public MetricsApiResponseHandlerImpl(Logger logger) {
        this.logger = logger;
    }

    /**
     * Handles logging an error message and throwing the appropriate exception when a
     * URIValidationException is caught in the client.
     *
     * @param companyNumber The company number for the delta that has come through on the topic.
     * @param deltaType The type of delta that has come through on the topic.
     * @param ex The exception that was caught in the client.
     */
    public void handle(String companyNumber, String deltaType, URIValidationException ex) {
        String message = String.format(FAILED_MSG, deltaType, companyNumber);
        logger.error(message);
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
    public void handle(String companyNumber, String deltaType, IllegalArgumentException ex) {
        String message = String.format(FAILED_MSG, deltaType, companyNumber);
        String causeMessage = ex.getCause() != null
                ? String.format("; %s", ex.getCause().getMessage()) : "";
        logger.info(message + causeMessage);
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
    public void handle(String companyNumber, String deltaType, ApiErrorResponseException ex) {
        String message = String.format(ERROR_MSG, ex.getStatusCode(), deltaType, companyNumber);
        if (HttpStatus.valueOf(ex.getStatusCode()).is5xxServerError()) {
            logger.info(message);
            throw new RetryableErrorException(message, ex);
        } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
            logger.info(message);
        } else {
            logger.error(message);
            throw new NonRetryableErrorException(message, ex);
        }
    }
}

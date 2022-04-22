package uk.gov.companieshouse.company.metrics.processor;

import java.util.Map;

import org.springframework.http.HttpStatus;

import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

public final class ResponseHandler {


    /**
     * Common response handler.
     */
    public static void handleResponse(
            final HttpStatus httpStatus,
            final String logContext,
            final String msg,
            final Map<String, Object> logMap,
            final Logger logger)
            throws NonRetryableErrorException, RetryableErrorException {
        logMap.put("status", httpStatus.toString());
        if (HttpStatus.BAD_REQUEST == httpStatus) {
            // 400 BAD REQUEST status is not retry-able
            logger.errorContext(logContext, msg, null, logMap);
            throw new NonRetryableErrorException(msg);
        } else if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
            // any other client or server status is retry-able
            logger.errorContext(logContext, msg + ", retry", null, logMap);
            throw new RetryableErrorException(msg);
        } else {
            logger.debugContext(logContext, msg, logMap);
        }
    }
}

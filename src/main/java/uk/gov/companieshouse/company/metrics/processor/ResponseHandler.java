package uk.gov.companieshouse.company.metrics.processor;

import java.util.Map;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableException;
import uk.gov.companieshouse.company.metrics.exception.RetryableException;
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
            throws NonRetryableException, RetryableException {
        logMap.put("status", httpStatus.toString());
        if (HttpStatus.BAD_REQUEST == httpStatus) {
            // 400 BAD REQUEST status is not retryable
            logger.errorContext(logContext, msg, null, logMap);
            throw new NonRetryableException(msg);
        } else if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
            // any other client or server status is retryable
            logger.errorContext(logContext, msg + ", retry", null, logMap);
            throw new RetryableException(msg);
        } else {
            logger.debugContext(logContext, msg, logMap);
        }
    }
}

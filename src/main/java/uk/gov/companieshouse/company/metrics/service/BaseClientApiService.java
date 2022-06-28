package uk.gov.companieshouse.company.metrics.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

public abstract class BaseClientApiService {
    protected final Logger logger;

    protected BaseClientApiService(Logger logger) {
        this.logger = logger;
    }

    /**
     * General execution of an SDK endpoint.
     *
     * @param <T>           type of api response
     * @param logContext    context ID for logging
     * @param operationName name of operation
     * @param uri           uri of sdk being called
     * @param executor      executor to use
     * @return the response object
     */
    public <T> ApiResponse<T> executeOp(final String logContext,
                                        final String operationName,
                                        final String uri,
                                        final Executor<ApiResponse<T>> executor) {

        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("operation_name", operationName);
        logMap.put("path", uri);

        try {
            return executor.execute();
        } catch (URIValidationException ex) {
            logger.errorContext(logContext, "SDK exception", ex, logMap);
            throw new RetryableErrorException("SDK Exception", ex);
        } catch (ApiErrorResponseException ex) {
            String message = "Private API Error Response exception";
            logMap.put("status", ex.getStatusCode());
            logger.errorContext(logContext, message, ex, logMap);
            if (ex.getStatusCode() != 0) {
                return new ApiResponse<>(ex.getStatusCode(), Collections.emptyMap());
            }
            throw new RetryableErrorException(message, ex);
        } catch (Exception ex) {
            String message = "Private API Generic exception";
            logger.errorContext(logContext, message, ex, logMap);
            throw new RetryableErrorException(message, ex);
        }
    }
}

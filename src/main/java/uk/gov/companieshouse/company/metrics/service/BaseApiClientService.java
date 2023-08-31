package uk.gov.companieshouse.company.metrics.service;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import java.util.Collections;
import java.util.Map;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public abstract class BaseApiClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    /**
     * General execution of an SDK endpoint.
     *
     * @param <T>           type of api response
     * @param operationName name of operation
     * @param uri           uri of sdk being called
     * @param executor      executor to use
     * @return the response object
     */
    public <T> ApiResponse<T> executeOp(final String operationName,
                                        final String uri,
                                        final Executor<ApiResponse<T>> executor) {

        final Map<String, Object> logMap = DataMapHolder.getLogMap();
        logMap.put("operation_name", operationName);
        logMap.put("path", uri);

        try {
            return executor.execute();
        } catch (URIValidationException ex) {
            LOGGER.error("SDK exception", ex, logMap);
            throw new RetryableErrorException("SDK Exception", ex);
        } catch (ApiErrorResponseException ex) {
            String message = "Private API Error Response exception";
            logMap.put("status", ex.getStatusCode());
            LOGGER.error(message, ex, logMap);
            if (ex.getStatusCode() != 0) {
                return new ApiResponse<>(ex.getStatusCode(), Collections.emptyMap());
            }
            throw new RetryableErrorException(message, ex);
        } catch (Exception ex) {
            String message = "Private API Generic exception";
            LOGGER.error(message, ex, logMap);
            throw new RetryableErrorException(message, ex);
        }
    }
}

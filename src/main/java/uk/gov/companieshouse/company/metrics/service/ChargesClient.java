package uk.gov.companieshouse.company.metrics.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
@Component
public class ChargesClient implements MetricsClient {

    private final Logger logger;

    private final Supplier<InternalApiClient> internalApiClientFactory;

    private final CompanyMetricsApiTransformer metricsApiTransformer;

    public ChargesClient(Logger logger, Supplier<InternalApiClient> internalApiClientFactory, CompanyMetricsApiTransformer metricsApiTransformer) {
        this.logger = logger;
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
    }


    @Override
    public void postMetrics(String companyNumber, String updatedBy){
        InternalApiClient client = internalApiClientFactory.get();
        try {
            MetricsRecalculateApi metricsRecalculateApi = metricsApiTransformer.transform(updatedBy);
            client.privateCompanyMetricsUpsertHandler()
                    .postCompanyMetrics(
                            String.format("/company/%s/metrics/recalculate", companyNumber), metricsRecalculateApi)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            if (ex.getStatusCode() / 100 == 5) {
                logger.error(String.format("Server error returned with status code: [%s] "
                        + "processing add officers link request", ex.getStatusCode()));
                throw new RetryableErrorException("Server error returned when processing "
                        + "add officers link request", ex);
            } else if (ex.getStatusCode() == 409) {
                logger.info("HTTP 409 Conflict returned; "
                        + "company profile already had officers link");
            } else if (ex.getStatusCode() == 404) {
                logger.info("HTTP 404 Not Found returned; "
                        + "company profile does not exist");
            } else {
                logger.error(String.format("Add officers client error returned with "
                                + "status code: [%s] when processing add officers link request",
                        ex.getStatusCode()));
                throw new NonRetryableErrorException("UpsertClient error returned when "
                        + "processing add officers link request", ex);
            }
        } catch (IllegalArgumentException ex) {
            logger.error("Illegal argument exception caught when handling API response");
            throw new RetryableErrorException("Server error returned when processing add "
                    + "officers link request", ex);
        } catch (URIValidationException ex) {
            logger.error("Invalid companyNumber specified when handling API request");
            throw new NonRetryableErrorException("Invalid companyNumber specified", ex);
        }
    }
}

package uk.gov.companieshouse.company.metrics.service;

import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;


@Component
public class DeletedChargesClient implements MetricsClient {

    private final Logger logger;

    private final Supplier<InternalApiClient> internalApiClientFactory;

    private final CompanyMetricsApiTransformer metricsApiTransformer;

    private final ChargesDataApiService chargesDataApiService;

    /**
     * Constructor to construct and return instance of
     * deleteChargesClient - used to post a recalculation of company charges metrics.
     *
     */
    public DeletedChargesClient(Logger logger,
                                Supplier<InternalApiClient> internalApiClientFactory,
                                CompanyMetricsApiTransformer metricsApiTransformer,
                                ChargesDataApiService chargesDataApiService) {
        this.logger = logger;
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.chargesDataApiService = chargesDataApiService;
    }

    @Override
    public void postMetrics(String companyNumber,
                            String updatedBy,
                            String resourceUri,
                            String contextId) {
        if (isChargeAlreadyDeleted(resourceUri, contextId)) {
            InternalApiClient client = internalApiClientFactory.get();
            try {
                MetricsRecalculateApi metricsRecalculateApi = metricsApiTransformer
                        .transform(updatedBy);
                client.privateCompanyMetricsUpsertHandler()
                        .postCompanyMetrics(
                                String.format("/company/%s/metrics/recalculate", companyNumber),
                                metricsRecalculateApi)
                        .execute();
            } catch (ApiErrorResponseException ex) {
                if (HttpStatus.valueOf(ex.getStatusCode()).is5xxServerError()) {
                    logger.error(String.format("Server error returned with status code: [%s] "
                            + "processing charges recalculate request", ex.getStatusCode()));
                    throw new RetryableErrorException("Server error returned when processing "
                            + "charges recalculate request", ex);
                } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                    logger.info("HTTP 404 Not Found returned; "
                            + "company does not exist");
                } else {
                    logger.error(String.format("Changed charges client error returned with "
                                    + "status code: [%s] when processing recalculate request",
                            ex.getStatusCode()));
                    throw new NonRetryableErrorException("UpsertClient error returned when "
                            + "processing charges recalculate request", ex);
                }
            } catch (IllegalArgumentException ex) {
                logger.error("Illegal argument exception caught when handling API response");
                throw new RetryableErrorException("Server error returned when processing "
                        + "charges recalculate request", ex);
            } catch (URIValidationException ex) {
                logger.error("Invalid companyNumber specified when handling API request");
                throw new NonRetryableErrorException("Invalid companyNumber specified", ex);
            }
        } else {
            throw new RetryableErrorException("Charge details found when should have been deleted");
        }
    }

    private boolean isChargeAlreadyDeleted(String resourceUri, String contextId) {
        ApiResponse<ChargeApi> apiResponseFromChargesDataApi = chargesDataApiService
                .getACharge(contextId, resourceUri);
        return apiResponseFromChargesDataApi.getStatusCode() == HttpStatus.NOT_FOUND.value();
    }
}

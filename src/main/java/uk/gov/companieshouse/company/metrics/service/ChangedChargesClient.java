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
public class ChangedChargesClient implements MetricsClient {

    public static final String FAILED_MSG = "Failed recalculating charges for company [%s]";
    public static final String ERROR_MSG = "Error [%s] recalculating charges for company [%s]";
    private final Logger logger;
    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final ChargesDataApiService chargesDataApiService;

    /**
     * Constructor to construct and return instance of
     * changedChargesClient - used to post a recalculation of company charges metrics.
     *
     */
    public ChangedChargesClient(Logger logger,
                                Supplier<InternalApiClient> internalApiClientFactory,
                                CompanyMetricsApiTransformer metricsApiTransformer,
                                ChargesDataApiService chargesDataApiService) {
        this.logger = logger;
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.chargesDataApiService = chargesDataApiService;
    }

    @Override
    public void postMetrics(String companyNumber, String updatedBy,
                            String resourceUri, String contextId) {
        if (isChargeAvailable(resourceUri, contextId)) {
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
                handleApiError(companyNumber, ex);
            } catch (IllegalArgumentException ex) {
                handleIllegalArgumentError(companyNumber, ex);
            } catch (URIValidationException ex) {
                handleUriValidationError(companyNumber, ex);
            }
        } else {
            throw new RetryableErrorException(String.format("Charge details not found for "
                    + "company [%s]", companyNumber));
        }
    }

    private void handleUriValidationError(String companyNumber, URIValidationException ex) {
        String message = String.format(FAILED_MSG, companyNumber);
        logger.error(message);
        throw new NonRetryableErrorException(message, ex);
    }

    private void handleIllegalArgumentError(String companyNumber, IllegalArgumentException ex) {
        String message = String.format(FAILED_MSG, companyNumber);
        logger.info(message);
        throw new RetryableErrorException(message, ex);
    }

    private void handleApiError(String companyNumber, ApiErrorResponseException ex) {
        String message = String.format(ERROR_MSG, ex.getStatusCode(), companyNumber);
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

    private boolean isChargeAvailable(String resourceUri, String contextId) {
        ApiResponse<ChargeApi> apiResponseFromChargesDataApi = chargesDataApiService
                .getACharge(contextId, resourceUri);
        if (apiResponseFromChargesDataApi == null) {
            return false;
        }
        return HttpStatus.valueOf(apiResponseFromChargesDataApi.getStatusCode()).is2xxSuccessful();
    }
}

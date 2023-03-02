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
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

@Component
public class ChangedChargesClient implements MetricsClient {

    private static final String CHARGES_DELTA_TYPE = "charges";
    private static final Boolean IS_MORTGAGE = true;
    private static final Boolean IS_APPOINTMENT = false;
    private static final Boolean IS_PSC = false;

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final ChargesDataApiService chargesDataApiService;
    private final MetricsApiResponseHandler metricsApiResponseHandlerImpl;

    /**
     * Constructor to construct and return instance of
     * changedChargesClient - used to post a recalculation of company charges metrics.
     *
     */
    public ChangedChargesClient(Supplier<InternalApiClient> internalApiClientFactory,
                                CompanyMetricsApiTransformer metricsApiTransformer,
                                ChargesDataApiService chargesDataApiService,
                                MetricsApiResponseHandler metricsApiResponseHandlerImpl) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.chargesDataApiService = chargesDataApiService;
        this.metricsApiResponseHandlerImpl = metricsApiResponseHandlerImpl;
    }

    @Override
    public void postMetrics(String companyNumber, String updatedBy,
                            String resourceUri, String contextId) {
        if (isChargeAvailable(resourceUri, contextId)) {
            InternalApiClient client = internalApiClientFactory.get();
            try {
                MetricsRecalculateApi metricsRecalculateApi = metricsApiTransformer
                        .transform(updatedBy, IS_MORTGAGE, IS_APPOINTMENT, IS_PSC);
                client.privateCompanyMetricsUpsertHandler()
                        .postCompanyMetrics(
                                String.format("/company/%s/metrics/recalculate", companyNumber),
                                metricsRecalculateApi)
                        .execute();
            } catch (ApiErrorResponseException ex) {
                metricsApiResponseHandlerImpl
                        .handle(companyNumber, CHARGES_DELTA_TYPE, ex, contextId);
            } catch (IllegalArgumentException ex) {
                metricsApiResponseHandlerImpl
                        .handle(companyNumber, CHARGES_DELTA_TYPE, ex, contextId);
            } catch (URIValidationException ex) {
                metricsApiResponseHandlerImpl
                        .handle(companyNumber, CHARGES_DELTA_TYPE, ex, contextId);
            }
        } else {
            throw new RetryableErrorException(String.format("Charge details not found for "
                    + "company [%s]", companyNumber));
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

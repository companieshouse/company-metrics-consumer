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
public class DeletedChargesClient implements MetricsClient {

    private static final String CHARGES_DELTA_TYPE = "charges";
    private static final boolean IS_MORTGAGE = true;
    private static final boolean IS_APPOINTMENT = false;
    private static final boolean IS_PSC = false;

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final ChargesDataApiService chargesDataApiService;
    private final ResponseHandler metricsApiResponseHandler;

    /**
     * Constructor to construct and return instance of
     * deleteChargesClient - used to post a recalculation of company charges metrics.
     *
     */
    public DeletedChargesClient(Supplier<InternalApiClient> internalApiClientFactory,
                                CompanyMetricsApiTransformer metricsApiTransformer,
                                ChargesDataApiService chargesDataApiService,
                                ResponseHandler metricsApiResponseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.chargesDataApiService = chargesDataApiService;
        this.metricsApiResponseHandler = metricsApiResponseHandler;
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
                        .transform(updatedBy, IS_MORTGAGE, IS_APPOINTMENT, IS_PSC);
                client.privateCompanyMetricsUpsertHandler()
                        .postCompanyMetrics(
                                String.format("/company/%s/metrics/recalculate", companyNumber),
                                metricsRecalculateApi)
                        .execute();
            } catch (ApiErrorResponseException ex) {
                metricsApiResponseHandler
                        .handle(companyNumber, CHARGES_DELTA_TYPE, ex, contextId);
            } catch (IllegalArgumentException ex) {
                metricsApiResponseHandler
                        .handle(companyNumber, CHARGES_DELTA_TYPE, ex, contextId);
            } catch (URIValidationException ex) {
                metricsApiResponseHandler
                        .handle(companyNumber, CHARGES_DELTA_TYPE, ex, contextId);
            }
        } else {
            throw new RetryableErrorException(String.format("Charge details found for [%s] when "
                    + "should have been deleted", companyNumber));
        }
    }

    private boolean isChargeAlreadyDeleted(String resourceUri, String contextId) {
        ApiResponse<ChargeApi> apiResponseFromChargesDataApi = chargesDataApiService
                .getACharge(contextId, resourceUri);
        return apiResponseFromChargesDataApi.getStatusCode() == HttpStatus.NOT_FOUND.value();
    }
}

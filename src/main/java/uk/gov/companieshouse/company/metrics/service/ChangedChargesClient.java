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
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

@Component
public class ChangedChargesClient implements MetricsClient {

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
     * changedChargesClient - used to post a recalculation of company charges metrics.
     *
     */
    public ChangedChargesClient(Supplier<InternalApiClient> internalApiClientFactory,
                                CompanyMetricsApiTransformer metricsApiTransformer,
                                ChargesDataApiService chargesDataApiService,
                                ResponseHandler metricsApiResponseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.chargesDataApiService = chargesDataApiService;
        this.metricsApiResponseHandler = metricsApiResponseHandler;
    }

    @Override
    public void postMetrics(String companyNumber, String updatedBy,
                            String resourceUri) {
        if (isChargeAvailable(resourceUri)) {
            InternalApiClient client = internalApiClientFactory.get();
            client.getHttpClient().setRequestId(DataMapHolder.getRequestId());

            try {
                MetricsRecalculateApi metricsRecalculateApi = metricsApiTransformer
                        .transform(updatedBy, IS_MORTGAGE, IS_APPOINTMENT, IS_PSC);
                client.privateCompanyMetricsUpsertHandler()
                        .postCompanyMetrics(
                                String.format("/company/%s/metrics/recalculate", companyNumber),
                                metricsRecalculateApi)
                        .execute();
            } catch (ApiErrorResponseException ex) {
                metricsApiResponseHandler.handle(companyNumber, CHARGES_DELTA_TYPE, ex);
            } catch (IllegalArgumentException ex) {
                metricsApiResponseHandler.handle(companyNumber, CHARGES_DELTA_TYPE, ex);
            } catch (URIValidationException ex) {
                metricsApiResponseHandler.handle(companyNumber, CHARGES_DELTA_TYPE, ex);
            }
        } else {
            throw new RetryableErrorException(String.format("Charge details not found for "
                    + "company [%s]", companyNumber));
        }
    }

    private boolean isChargeAvailable(String resourceUri) {
        ApiResponse<ChargeApi> apiResponseFromChargesDataApi = chargesDataApiService
                .getACharge(resourceUri);
        if (apiResponseFromChargesDataApi == null) {
            return false;
        }
        return HttpStatus.valueOf(apiResponseFromChargesDataApi.getStatusCode()).is2xxSuccessful();
    }
}

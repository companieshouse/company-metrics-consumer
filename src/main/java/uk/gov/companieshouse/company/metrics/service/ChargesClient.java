package uk.gov.companieshouse.company.metrics.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

@Component("chargesClient")
public class ChargesClient implements MetricsClient {

    private static final String CHARGES_DELTA_TYPE = "charges";
    private static final boolean IS_MORTGAGE = true;
    private static final boolean IS_APPOINTMENT = false;
    private static final boolean IS_PSC = false;
    private static final boolean IS_REGISTERS = false;

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final ResponseHandler metricsApiResponseHandler;

    /**
     * Constructor to construct and return instance of
     * changedChargesClient - used to post a recalculation of company charges metrics.
     */
    public ChargesClient(Supplier<InternalApiClient> internalApiClientFactory,
                         CompanyMetricsApiTransformer metricsApiTransformer,
                         ResponseHandler metricsApiResponseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.metricsApiResponseHandler = metricsApiResponseHandler;
    }

    @Override
    public void postMetrics(String companyNumber, String updatedBy,
                            String resourceUri) {
        InternalApiClient client = internalApiClientFactory.get();
        client.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        try {
            MetricsRecalculateApi metricsRecalculateApi = metricsApiTransformer
                    .transform(updatedBy, IS_MORTGAGE, IS_APPOINTMENT, IS_PSC, IS_REGISTERS);
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
    }
}

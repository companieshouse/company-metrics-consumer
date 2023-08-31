package uk.gov.companieshouse.company.metrics.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

@Component
public class AppointmentsClient implements MetricsClient {

    private static final String APPOINTMENTS_DELTA_TYPE = "appointments";
    private static final boolean IS_MORTGAGE = false;
    private static final boolean IS_APPOINTMENT = true;
    private static final boolean IS_PSC = false;

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final ResponseHandler metricsApiResponseHandler;

    /**
     * Constructs AppointmentsClient object. This object is used to send a POST request to the
     * recalculation endpoint in company-metrics-api.
     */
    public AppointmentsClient(Supplier<InternalApiClient> internalApiClientFactory,
                              CompanyMetricsApiTransformer metricsApiTransformer,
                              ResponseHandler metricsApiResponseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.metricsApiResponseHandler = metricsApiResponseHandler;
    }

    /**
     * Sends a POST request to the metrics recalculate endpoint in the
     * company metrics api and handles any error responses.
     *
     * @param companyNumber The companyNumber of the POST request
     */
    @Override
    public void postMetrics(String companyNumber, String updatedBy,
                            String resourceUri) {
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
            metricsApiResponseHandler
                    .handle(companyNumber, APPOINTMENTS_DELTA_TYPE, ex);
        } catch (IllegalArgumentException ex) {
            metricsApiResponseHandler
                    .handle(companyNumber, APPOINTMENTS_DELTA_TYPE, ex);
        } catch (URIValidationException ex) {
            metricsApiResponseHandler
                    .handle(companyNumber, APPOINTMENTS_DELTA_TYPE, ex);
        }
    }
}

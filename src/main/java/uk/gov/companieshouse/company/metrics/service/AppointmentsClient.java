package uk.gov.companieshouse.company.metrics.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

@Component
public class AppointmentsClient implements MetricsClient {

    public static final String APPOINTMENTS_DELTA_TYPE = "appointments";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final MetricsApiResponseHandler metricsApiResponseHandlerImpl;

    /**
     * Constructs AppointmentsClient object. This object is used to send a POST request to the
     * recalculation endpoint in company-metrics-api.
     */
    public AppointmentsClient(Supplier<InternalApiClient> internalApiClientFactory,
                              CompanyMetricsApiTransformer metricsApiTransformer,
                              MetricsApiResponseHandlerImpl metricsApiResponseHandlerImpl) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.metricsApiTransformer = metricsApiTransformer;
        this.metricsApiResponseHandlerImpl = metricsApiResponseHandlerImpl;
    }

    /**
     * Sends a POST request to the metrics recalculate endpoint in the
     * company metrics api and handles any error responses.
     *
     * @param companyNumber The companyNumber of the POST request
     */
    @Override
    public void postMetrics(String companyNumber, String updatedBy,
                            String resourceUri, String contextId) {
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
            metricsApiResponseHandlerImpl.handle(companyNumber, APPOINTMENTS_DELTA_TYPE, ex);
        } catch (IllegalArgumentException ex) {
            metricsApiResponseHandlerImpl.handle(companyNumber, APPOINTMENTS_DELTA_TYPE, ex);
        } catch (URIValidationException ex) {
            metricsApiResponseHandlerImpl.handle(companyNumber, APPOINTMENTS_DELTA_TYPE, ex);
        }
    }
}
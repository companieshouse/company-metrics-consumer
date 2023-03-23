package uk.gov.companieshouse.company.metrics.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

@Component
public class PscStatementsClient implements MetricsClient {

    private static final String PSC_STATEMENTS_DELTA_TYPE = "psc-statements";
    private static final boolean IS_MORTGAGE = false;
    private static final boolean IS_APPOINTMENT = false;
    private static final boolean IS_PSC = true;

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final ResponseHandler metricsApiResponseHandler;

    /**
     * Constructs PscStatementsClient object. This object is used to send a POST request to the
     * recalculation endpoint in company-metrics-api.
     */
    public PscStatementsClient(Supplier<InternalApiClient> internalApiClientFactory,
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
                            String resourceUri, String contextId) {
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
                    .handle(companyNumber, PSC_STATEMENTS_DELTA_TYPE, ex, contextId);
        } catch (IllegalArgumentException ex) {
            metricsApiResponseHandler
                    .handle(companyNumber, PSC_STATEMENTS_DELTA_TYPE, ex, contextId);
        } catch (URIValidationException ex) {
            metricsApiResponseHandler
                    .handle(companyNumber, PSC_STATEMENTS_DELTA_TYPE, ex, contextId);
        }
    }
}

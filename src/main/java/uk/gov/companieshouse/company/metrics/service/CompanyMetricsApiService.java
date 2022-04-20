package uk.gov.companieshouse.company.metrics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.service.api.BaseApiClientServiceImpl;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Service
public class CompanyMetricsApiService extends BaseApiClientServiceImpl {

    @Value("${api.company-profile-api-key}")
    private String companyProfileApiKey;

    @Value("${api.endpoint}")
    private String companyProfileApiUrl;

    @Autowired
    public CompanyMetricsApiService(Logger logger) {
        super(logger);
    }

    /**
     * Invoke Company Metrics API.
     */
    public ApiResponse<?> invokeCompanyMetricsApi() {
        InternalApiClient internalApiClient = getInternalApiClient();
        internalApiClient.setBasePath("apiUrl");

        return null;
    }

    public InternalApiClient getApiClient(String contextId) {
        InternalApiClient apiClient = new InternalApiClient(getHttpClient(contextId));
        apiClient.setBasePath("apiUrl");
        return apiClient;
    }

    private HttpClient getHttpClient(String contextId) {
        ApiKeyHttpClient httpClient = new ApiKeyHttpClient(companyProfileApiKey);
        httpClient.setRequestId(contextId);
        return httpClient;
    }

    /**
     * POST a company profile given a company number extracted in CompanyMetricsProcessor.
     *
     * @param companyNumber the company's company number
     * @return an ApiResponse containing the metrics recalculate api data model
     */
    public ApiResponse<Void> postCompanyMetrics(String contextId, String companyNumber,
                                                MetricsRecalculateApi metricsRecalculateApi) {
        String uri = String.format("/company/%s/metrics/recalculate", companyNumber);

        Map<String, Object> logMap = createLogMap(companyNumber, "POST", uri);
        logger.infoContext(contextId, String.format("POST %s", uri), logMap);

        return executeOp(contextId, "recalculate", uri,
                getApiClient(contextId)
                        .privateCompanyMetricsUpsertHandler()
                        .postCompanyMetrics(uri, metricsRecalculateApi));
    }

    private Map<String, Object> createLogMap(String companyNumber, String method, String path) {
        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("company_number", companyNumber);
        logMap.put("method", method);
        logMap.put("path", path);
        return logMap;
    }

    @Lookup
    public InternalApiClient getInternalApiClient() {
        return null;
    }
}

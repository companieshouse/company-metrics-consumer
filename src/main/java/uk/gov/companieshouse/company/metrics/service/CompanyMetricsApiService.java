package uk.gov.companieshouse.company.metrics.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsUpsert;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@Service
public class CompanyMetricsApiService extends BaseClientApiService {

    private final Supplier<InternalApiClient> internalApiClientSupplier;


    /**
     * Constructor that takes in a Supplier to be able to construct and return internalApiClient
     * based on the provided key and url for the api call.
     */
    @Autowired
    public CompanyMetricsApiService(Logger logger,
                                    Supplier<InternalApiClient> internalApiClientSupplier) {
        super(logger);
        this.internalApiClientSupplier = internalApiClientSupplier;
    }

    /**
     * POST a company metrics api to recalculate the count given a company number
     * extracted in CompanyMetricsProcessor.
     *
     * @return ApiResponse
     */
    public ApiResponse<Void> invokeMetricsPostApi(String contextId,
                                                  String companyNumber,
                                                  MetricsRecalculateApi metricsRecalculateApi) {
        String uri = String.format("/company/%s/metrics/recalculate", companyNumber);

        Map<String, Object> logMap = createLogMap(companyNumber, "POST", uri);
        logger.infoContext(contextId, String.format("POST %s", uri), logMap);

        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(contextId);
        PrivateCompanyMetricsUpsert metricsUpsert =
                internalApiClient.privateCompanyMetricsUpsertHandler()
                        .postCompanyMetrics(uri, metricsRecalculateApi);

        return executeOp(contextId, "recalculate", uri, metricsUpsert);
    }

    /**
     * Create Log context map.
     */
    private Map<String, Object> createLogMap(String companyNumber, String method, String path) {
        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("company_number", companyNumber);
        logMap.put("method", method);
        logMap.put("path", path);
        return logMap;
    }

}

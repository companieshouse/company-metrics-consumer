package uk.gov.companieshouse.company.metrics.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesGet;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesDataApiService extends BaseClientApiService {

    private final BiFunction<String, String, InternalApiClient> internalApiClientSupplier;
    private String apiKey;
    private String apiUrl;

    /**
     * Constructor that takes in a BiFunction to be able to construct and return internalApiClient
     * based on the provided key and url for the api call.
     */
    @Autowired
    public ChargesDataApiService(Logger logger,
                                 BiFunction<String, String,
                                         InternalApiClient> internalApiClientSupplier,
                                 @Value("${api.company-metrics-api-key}") String apiKey,
                                 @Value("${api.api-url}") String apiUrl) {
        super(logger);
        this.internalApiClientSupplier = internalApiClientSupplier;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    /**
     * POST a company metrics api to recalculate the count given a company number
     * extracted in CompanyMetricsProcessor.
     *
     * @return ApiResponse
     */
    public ApiResponse<ChargeApi> getACharge(String contextId,
                                            String uri) {
        Map<String, Object> logMap = createLogMap("GET", uri);
        logger.infoContext(contextId, String.format("GET %s", uri), logMap);

        InternalApiClient internalApiClient = internalApiClientSupplier
                .apply(this.apiKey, this.apiUrl);
        internalApiClient.getHttpClient().setRequestId(contextId);
        PrivateChargesGet privateChargesGet =
                internalApiClient.privateDeltaChargeResourceHandler()
                        .getACharge(uri);

        return executeOp(contextId, "get", uri, privateChargesGet);
    }

    /**
     * Create Log context map.
     */
    private Map<String, Object> createLogMap(String method, String path) {
        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("method", method);
        logMap.put("path", path);
        return logMap;
    }

}

package uk.gov.companieshouse.company.metrics.service;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesGet;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ChargesDataApiService extends BaseApiClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final Supplier<InternalApiClient> internalApiClientSupplier;

    /**
     * Constructor that takes in a BiFunction to be able to construct and return internalApiClient
     * based on the provided key and url for the api call.
     */
    public ChargesDataApiService(Supplier<InternalApiClient> internalApiClientSupplier) {
        this.internalApiClientSupplier = internalApiClientSupplier;

    }

    /**
     * POST a company metrics api to recalculate the count given a company number
     * extracted in CompanyMetricsProcessor.
     *
     * @return ApiResponse
     */
    public ApiResponse<ChargeApi> getACharge(String uri) {
        Map<String, Object> logMap = createLogMap(uri);
        LOGGER.info(String.format("GET %s", uri), logMap);

        InternalApiClient internalApiClient = internalApiClientSupplier.get();
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());

        PrivateChargesGet privateChargesGet =
                internalApiClient.privateDeltaChargeResourceHandler()
                        .getACharge(uri);

        return executeOp("get", uri, privateChargesGet);
    }

    /**
     * Create Log context map.
     */
    private Map<String, Object> createLogMap(String path) {
        final Map<String, Object> logMap = DataMapHolder.getLogMap();
        logMap.put("method", HttpMethod.GET.name());
        logMap.put("path", path);
        return logMap;
    }
}

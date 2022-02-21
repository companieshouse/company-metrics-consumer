package uk.gov.companieshouse.company.metrics.service;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class CompanyMetricsApiService {

    /**
     * Invoke Insolvency API.
     */
    public ApiResponse<?> invokeInsolvencyApi() {
        InternalApiClient internalApiClient = getInternalApiClient();
        internalApiClient.setBasePath("apiUrl");

        return null;
    }

    @Lookup
    public InternalApiClient getInternalApiClient() {
        return null;
    }
}

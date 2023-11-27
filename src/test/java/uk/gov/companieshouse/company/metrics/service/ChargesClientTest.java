package uk.gov.companieshouse.company.metrics.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.PrivateCompanyMetricsUpsertHandler;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsUpsert;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import java.util.Collections;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ChargesClientTest {

    private static final String COMPANY_NUMBER = "01203396";
    private static final String PATH = String.format("/company/%s/metrics/recalculate", COMPANY_NUMBER);
    private static final String UPDATED_BY = "updatedBy";
    private static final String RESOURCE_URI = String.format("/company/%s/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg", COMPANY_NUMBER);
    private static final String CHARGES_DELTA_TYPE = "charges";
    private static final String INVALID_PATH = "invalid/path";
    private static final boolean IS_MORTGAGE = true;
    private static final boolean IS_APPOINTMENTS = false;
    private static final boolean IS_PSC = false;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private HttpClient httpClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler chargesMetricsPostHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;

    @Mock
    private CompanyMetricsApiTransformer metricsApiTransformer;

    @Mock
    private MetricsApiResponseHandler metricsApiResponseHandler;

    @InjectMocks
    private ChargesClient client;

    @Test
    void testUpsert() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));


        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, true, false, false));
        verify(privateCompanyMetricsUpsert).execute();
    }

    @Test
    void testThrowNonRetryableExceptionIfClientErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(404, "Not found", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, true, false, false));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowRetryableExceptionIfServerErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(500, "Internal server error", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, true, false, false));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowNonRetryableExceptionIfBadRequestReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(400, "Internal server error", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowRetryableExceptionIfIllegalArgumentExceptionIsCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Internal server error");
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(illegalArgumentException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, true, false, false));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, illegalArgumentException);
    }

    @Test
    void testThrowNonRetryableExceptionIfComapnyNumberInvalid() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(uriValidationException);

        // when
        client.postMetrics(INVALID_PATH, UPDATED_BY, RESOURCE_URI);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics("/company/invalid/path/metrics/recalculate", metricsApiTransformer.transform(UPDATED_BY, true, false, false));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(INVALID_PATH, CHARGES_DELTA_TYPE, uriValidationException);
    }
}

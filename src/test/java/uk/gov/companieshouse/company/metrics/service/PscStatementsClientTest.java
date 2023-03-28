package uk.gov.companieshouse.company.metrics.service;

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
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;

import java.util.Collections;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PscStatementsClientTest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String PATH = String.format("/company/%s/metrics/recalculate", COMPANY_NUMBER);
    private static final String UPDATED_BY = "updated_by";
    private static final String RESOURCE_URI = "resource_uri";
    private static final String CONTEXT_ID = "context_id";
    private static final String PSC_STATEMENTS_DELTA_TYPE = "psc-statements";
    private static final boolean IS_MORTGAGE = false;
    private static final boolean IS_APPOINTMENTS = false;
    private static final boolean IS_PSC = true;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler pscStatementsMetricsPostHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;

    @Mock
    private CompanyMetricsApiTransformer metricsApiTransformer;

    @Mock
    private MetricsApiResponseHandler metricsApiResponseHandler;

    @InjectMocks
    private PscStatementsClient client;

    @Test
    void testSuccessfulPostRequest() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(pscStatementsMetricsPostHandler);
        when(pscStatementsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(pscStatementsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC));
        verify(privateCompanyMetricsUpsert).execute();
    }

    @Test
    void testThrowRetryableExceptionIfServerErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(500, "Internal server error", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(pscStatementsMetricsPostHandler);
        when(pscStatementsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(pscStatementsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, PSC_STATEMENTS_DELTA_TYPE, apiErrorResponseException, CONTEXT_ID);
    }

    @Test
    void testThrowNonRetryableExceptionIfClientErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(404, "Not found", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(pscStatementsMetricsPostHandler);
        when(pscStatementsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(pscStatementsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, PSC_STATEMENTS_DELTA_TYPE, apiErrorResponseException, CONTEXT_ID);
    }

    @Test
    void testThrowNonRetryableExceptionIfIllegalArgumentExceptionCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Internal server error");
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(pscStatementsMetricsPostHandler);
        when(pscStatementsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(illegalArgumentException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(pscStatementsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, PSC_STATEMENTS_DELTA_TYPE, illegalArgumentException, CONTEXT_ID);
    }

    @Test
    void testThrowNonRetryableExceptionIfUriValidationExceptionCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Internal server error");
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(pscStatementsMetricsPostHandler);
        when(pscStatementsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(uriValidationException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(pscStatementsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, PSC_STATEMENTS_DELTA_TYPE, uriValidationException, CONTEXT_ID);
    }
}
package uk.gov.companieshouse.company.metrics.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.PrivateCompanyMetricsUpsertHandler;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsUpsert;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;

import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentsClientTest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String PATH = String.format("/company/%s/metrics/recalculate", COMPANY_NUMBER);
    private static final String UPDATED_BY = "updated_by";
    private static final String RESOURCE_URI = "resource_uri";
    private static final String CONTEXT_ID = "context_id";
    public static final String FAILED_MSG = "Failed recalculating appointments for company [%s]";
    public static final String ERROR_MSG = "Error [%s] recalculating appointments for company [%s]";

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler appointmentsMetricsPostHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;

    @Mock
    private CompanyMetricsApiTransformer metricsApiTransformer;

    @Mock
    private Logger logger;

    @InjectMocks
    private AppointmentsClient client;

    @Test
    void testSuccessfulPostRequest() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(appointmentsMetricsPostHandler);
        when(appointmentsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY));
        verify(privateCompanyMetricsUpsert).execute();
    }

    @Test
    void testThrowRetryableExceptionIfServerErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(appointmentsMetricsPostHandler);
        when(appointmentsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(new ApiErrorResponseException(new HttpResponseException.Builder(500, "Internal server error", new HttpHeaders())));

        // when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        assertThrows(RetryableErrorException.class, actual);
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(logger).info(String.format(ERROR_MSG, "500", COMPANY_NUMBER));
    }

    @Test
    void testThrowNonRetryableExceptionIfClientErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(appointmentsMetricsPostHandler);
        when(appointmentsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(new ApiErrorResponseException(new HttpResponseException.Builder(404, "Not found", new HttpHeaders())));

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(logger).info(String.format(ERROR_MSG, "404", COMPANY_NUMBER));
    }

    @Test
    void testThrowNonRetryableExceptionIfIllegalArgumentExceptionCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(appointmentsMetricsPostHandler);
        when(appointmentsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(new IllegalArgumentException("Internal server error"));

        // when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        assertThrows(RetryableErrorException.class, actual);
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(logger).info(String.format(FAILED_MSG, COMPANY_NUMBER));
    }

    @Test
    void testThrowNonRetryableExceptionIfUriValidationExceptionCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(appointmentsMetricsPostHandler);
        when(appointmentsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(new URIValidationException("Internal server error"));

        // when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        // then
        assertThrows(NonRetryableErrorException.class, actual);
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(logger).error(String.format(FAILED_MSG, COMPANY_NUMBER));
    }
}
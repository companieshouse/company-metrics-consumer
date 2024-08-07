package uk.gov.companieshouse.company.metrics.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.Collections;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class AppointmentsClientTest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String PATH = String.format("/company/%s/metrics/recalculate", COMPANY_NUMBER);
    private static final String UPDATED_BY = "updated_by";
    private static final String RESOURCE_URI = "resource_uri";
    private static final String APPOINTMENTS_DELTA_TYPE = "appointments";
    private static final boolean IS_MORTGAGE = false;
    private static final boolean IS_APPOINTMENTS = true;
    private static final boolean IS_PSC = false;
    private static final boolean IS_REGISTERS = false;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private HttpClient httpClient;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler appointmentsMetricsPostHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;

    @Mock
    private CompanyMetricsApiTransformer metricsApiTransformer;

    @Mock
    private MetricsApiResponseHandler metricsApiResponseHandler;

    @InjectMocks
    private AppointmentsClient client;

    @BeforeEach
    void setup() {
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(appointmentsMetricsPostHandler);
        when(appointmentsMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
    }

    @Test
    void testSuccessfulPostRequest() throws ApiErrorResponseException, URIValidationException {
        // given
        when(privateCompanyMetricsUpsert.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC, IS_REGISTERS));
        verify(privateCompanyMetricsUpsert).execute();
    }

    @Test
    void testThrowRetryableExceptionIfServerErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(500, "Internal server error", new HttpHeaders()));
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC, IS_REGISTERS));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowNonRetryableExceptionIfClientErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(404, "Not found", new HttpHeaders()));
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC, IS_REGISTERS));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowNonRetryableExceptionIfIllegalArgumentExceptionCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Internal server error");
        when(privateCompanyMetricsUpsert.execute()).thenThrow(illegalArgumentException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC, IS_REGISTERS));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException);
    }

    @Test
    void testThrowNonRetryableExceptionIfUriValidationExceptionCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Internal server error");
        when(privateCompanyMetricsUpsert.execute()).thenThrow(uriValidationException);

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI);

        // then
        verify(appointmentsMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATED_BY, IS_MORTGAGE, IS_APPOINTMENTS, IS_PSC, IS_REGISTERS));
        verify(privateCompanyMetricsUpsert).execute();
        verify(metricsApiResponseHandler).handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, uriValidationException);
    }
}
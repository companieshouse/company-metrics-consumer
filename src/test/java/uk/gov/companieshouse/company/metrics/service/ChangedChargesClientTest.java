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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangedChargesClientTest {

    private static final String COMPANY_NUMBER = "01203396";
    private static final String PATH = String.format("/company/%s/metrics/recalculate", COMPANY_NUMBER);

    private static final String UPDATEDBY = "updatedBy";

    private static final String RESOURCEURI = String.format("/company/%s/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg", COMPANY_NUMBER);

    private static final String CONTEXTID = "context_id";
    private static final String CHARGES_DELTA_TYPE = "charges";
    private static final String INVALID_PATH = "invalid/path";

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler chargesMetricsPostHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;

    @Mock
    private CompanyMetricsApiTransformer metricsApiTransformer;

    @Mock
    private ChargesDataApiService chargesDataApiService;

    @Mock
    private MetricsApiResponseHandlerImpl responseHandler;

    @InjectMocks
    private ChangedChargesClient client;


    @Test
    void testUpsert() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));


        // when
        client.postMetrics(COMPANY_NUMBER, UPDATEDBY, RESOURCEURI, CONTEXTID);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATEDBY));
        verify(privateCompanyMetricsUpsert).execute();
    }

    @Test
    void testThrowNonRetryableExceptionIfClientErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(404, "Not found", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(COMPANY_NUMBER,UPDATEDBY, RESOURCEURI, CONTEXTID);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATEDBY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(responseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowRetryableExceptionIfServerErrorReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(new HttpResponseException.Builder(500, "Internal server error", new HttpHeaders()));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(apiErrorResponseException);
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATEDBY, RESOURCEURI, CONTEXTID);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATEDBY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(responseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, apiErrorResponseException);
    }

    @Test
    void testThrowNonRetryableExceptionIfBadRequestReturned() throws ApiErrorResponseException, URIValidationException {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(new ApiErrorResponseException(new HttpResponseException.Builder(400, "Internal server error", new HttpHeaders())));
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATEDBY, RESOURCEURI, CONTEXTID);

        // then
        assertThrows(NonRetryableErrorException.class, actual);
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATEDBY));
        verify(privateCompanyMetricsUpsert).execute();
    }

    @Test
    void testThrowRetryableExceptionIfIllegalArgumentExceptionIsCaught() throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Internal server error");
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(illegalArgumentException);
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(COMPANY_NUMBER, UPDATEDBY, RESOURCEURI, CONTEXTID);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics(PATH, metricsApiTransformer.transform(UPDATEDBY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(responseHandler).handle(COMPANY_NUMBER, CHARGES_DELTA_TYPE, illegalArgumentException);
    }

    @Test
    void testThrowNonRetryableExceptionIfComapnyNumberInvalid() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(chargesMetricsPostHandler);
        when(chargesMetricsPostHandler.postCompanyMetrics(anyString(), any())).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenThrow(uriValidationException);
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        client.postMetrics(INVALID_PATH, UPDATEDBY, RESOURCEURI, CONTEXTID);

        // then
        verify(chargesMetricsPostHandler).postCompanyMetrics("/company/invalid/path/metrics/recalculate", metricsApiTransformer.transform(UPDATEDBY));
        verify(privateCompanyMetricsUpsert).execute();
        verify(responseHandler).handle(INVALID_PATH, CHARGES_DELTA_TYPE, uriValidationException);
    }

    @Test
    void testThrowRetryableExceptionIfChargesDetailsCannotBeFound() throws ApiErrorResponseException, URIValidationException{
        //given
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(404, Collections.emptyMap()));


        // when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATEDBY, RESOURCEURI, CONTEXTID);

        //then
        Exception exception = assertThrows(RetryableErrorException.class, actual);
        String expectedMessage = "Charge details not found for company [01203396]";
        assertEquals(expectedMessage, exception.getMessage());
        verifyNoInteractions(chargesMetricsPostHandler);
        verifyNoInteractions(privateCompanyMetricsUpsert);
    }

    @Test
    void testThrowsRetryableExceptionIfChargeDataApiReturnsNullResponse() throws ApiErrorResponseException, URIValidationException {
        //given
        when(chargesDataApiService.getACharge(CONTEXTID, RESOURCEURI)).thenReturn(new ApiResponse<>(null));

        // when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATEDBY, RESOURCEURI, CONTEXTID);

        //then
        Exception exception = assertThrows(RetryableErrorException.class, actual);
        String expectedMessage = "Charge details not found for company [01203396]";
        assertEquals(expectedMessage, exception.getMessage());
        verifyNoInteractions(chargesMetricsPostHandler);
        verifyNoInteractions(privateCompanyMetricsUpsert);
    }
}
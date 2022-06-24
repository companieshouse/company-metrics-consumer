package uk.gov.companieshouse.company.metrics.service;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.PrivateCompanyMetricsUpsertHandler;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsUpsert;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.util.TestSupport;
import uk.gov.companieshouse.logging.Logger;

import java.util.Collections;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.company.metrics.util.TestSupport.buildApiErrorResponseCustomException;
import static uk.gov.companieshouse.company.metrics.util.TestSupport.buildApiErrorResponseException;

@ExtendWith(MockitoExtension.class)
public class CompanyMetricsApiServiceTest {

    private static final String MOCK_CONTEXT_ID = "context_id";
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_COMPANY_METRICS_RECALCULATE_URI = String.format("/company/%s/metrics/recalculate",
            MOCK_COMPANY_NUMBER);
    private static final String MOCK_TOPIC = "topic";
    private static final String MOCK_PARTITION = "partition";
    private static final String MOCK_OFFSET = "offset";

    private CompanyMetricsApiService companyMetricsApiService;

    @Mock
    private Logger logger;

    @Mock
    private BiFunction<String, String, InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient mockInternalApiClient;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler privateCompanyMetricsUpsertHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;

    TestSupport testSupport = new TestSupport();

    @BeforeEach
    void setup() {
        companyMetricsApiService = spy(new CompanyMetricsApiService(logger, internalApiClientSupplier, "metricsApiKey", "metricsApiUrl"));
        when(internalApiClientSupplier.apply("metricsApiKey", "metricsApiUrl")).thenReturn(mockInternalApiClient);
        when(mockInternalApiClient.getHttpClient()).thenReturn(mockHttpClient);
        when(mockInternalApiClient.privateCompanyMetricsUpsertHandler()).thenReturn(privateCompanyMetricsUpsertHandler);
    }

    @Test
    @DisplayName("Successfully send a POST request with a company metrics")
    void postCompanyMetrics() throws ApiErrorResponseException, URIValidationException {

        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);

        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);

        final ApiResponse<Void> expected = new ApiResponse<>(
                HttpStatus.OK.value(), Collections.emptyMap());

        when(privateCompanyMetricsUpsertHandler.postCompanyMetrics(MOCK_COMPANY_METRICS_RECALCULATE_URI, metricsRecalculateApi)).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenReturn(expected);

        final ApiResponse<Void> response = companyMetricsApiService.invokeMetricsPostApi(
                MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, metricsRecalculateApi);

        assertThat(response).isEqualTo(expected);
    }


    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, buildApiErrorResponseException(HttpStatus.BAD_REQUEST)),
                Arguments.of(HttpStatus.NOT_FOUND, buildApiErrorResponseException(HttpStatus.NOT_FOUND)),
                Arguments.of(HttpStatus.UNAUTHORIZED, buildApiErrorResponseException(HttpStatus.UNAUTHORIZED)),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, buildApiErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR)),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, buildApiErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR))
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    @DisplayName("When calling Metrics api and an error occurs then throw the appropriate exception based on the error type")
    void When_ValidMessage_but_backendApiService_throws_appropriate_error(HttpStatus httpStatus,
                                                                          ApiErrorResponseException exceptionFromApi)
            throws ApiErrorResponseException, URIValidationException {

        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);

        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);

        when(privateCompanyMetricsUpsertHandler.postCompanyMetrics(
                MOCK_COMPANY_METRICS_RECALCULATE_URI,
                metricsRecalculateApi))
                .thenReturn(privateCompanyMetricsUpsert);

        when(privateCompanyMetricsUpsert.execute()).thenThrow(exceptionFromApi);

        ApiResponse<?> apiResponse =
                companyMetricsApiService.invokeMetricsPostApi(
                        MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, metricsRecalculateApi);

        Assertions.assertThat(apiResponse).isNotNull();
        Assertions.assertThat(apiResponse.getStatusCode()).isEqualTo(httpStatus.value());
    }

    @Test
    @DisplayName("When calling metrics api and URLValidation exception occurs then throw the appropriate exception based on the error type")
    void When_ValidMessage_but_backendApiService_throws_URLValidation()
            throws ApiErrorResponseException, URIValidationException {

        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);

        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);

        when(privateCompanyMetricsUpsertHandler.postCompanyMetrics(
                MOCK_COMPANY_METRICS_RECALCULATE_URI,
                metricsRecalculateApi))
                .thenReturn(privateCompanyMetricsUpsert);

        when(privateCompanyMetricsUpsert.execute()).thenThrow(URIValidationException.class);

        assertThrows(RetryableErrorException.class, () ->
                companyMetricsApiService.invokeMetricsPostApi(
                        MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, metricsRecalculateApi));

    }

    @Test
    @DisplayName("When calling metrics api and APIResponse returns 0 status code")
    void When_ValidMessage_but_backendApiService_throws_error_with_status_code_0()
            throws ApiErrorResponseException, URIValidationException {

        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);

        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);

        when(privateCompanyMetricsUpsertHandler.postCompanyMetrics(
                MOCK_COMPANY_METRICS_RECALCULATE_URI,
                metricsRecalculateApi))
                .thenReturn(privateCompanyMetricsUpsert);

        when(privateCompanyMetricsUpsert.execute()).thenThrow(buildApiErrorResponseCustomException(0));

        assertThrows(RetryableErrorException.class, () ->
                companyMetricsApiService.invokeMetricsPostApi(
                        MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, metricsRecalculateApi));

    }
}

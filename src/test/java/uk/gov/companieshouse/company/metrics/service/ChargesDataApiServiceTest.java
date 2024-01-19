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
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.delta.PrivateDeltaResourceHandler;
import uk.gov.companieshouse.api.handler.delta.charges.request.PrivateChargesGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import uk.gov.companieshouse.api.http.HttpClient;

import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.util.TestSupport;
import uk.gov.companieshouse.logging.Logger;

import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.company.metrics.util.TestSupport.buildApiErrorResponseCustomException;
import static uk.gov.companieshouse.company.metrics.util.TestSupport.buildApiErrorResponseException;

@ExtendWith(MockitoExtension.class)
class ChargesDataApiServiceTest {

    private static final String MOCK_CONTEXT_ID = "context_id";
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CHARGE_ID = "MYdKM_YnzAmJ8JtSgVXr61n1bgg";
    private static final String MOCK_GET_CHARGE_URI = String.format("/company/%s/charges/%s",
            MOCK_COMPANY_NUMBER, MOCK_CHARGE_ID);

    private ChargesDataApiService chargesDataApiService;

    @Mock
    private Logger logger;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient mockInternalApiClient;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private PrivateDeltaResourceHandler privateDeltaResourceHandler;

    @Mock
    private PrivateChargesGet privateChargesGet;

    TestSupport testSupport = new TestSupport();

    @BeforeEach
    void setup() {
        chargesDataApiService = spy(new ChargesDataApiService(logger, internalApiClientSupplier));
        when(internalApiClientSupplier.get()).thenReturn(mockInternalApiClient);
        when(mockInternalApiClient.getHttpClient()).thenReturn(mockHttpClient);
        when(mockInternalApiClient.privateDeltaChargeResourceHandler()).thenReturn(privateDeltaResourceHandler);
    }

    @Test
    @DisplayName("Successfully send a GET request to charges data api")
    void getCharge() throws ApiErrorResponseException, URIValidationException {

        final ApiResponse<ChargeApi> expected = new ApiResponse<>(
                HttpStatus.OK.value(), Collections.emptyMap());

        when(privateDeltaResourceHandler.getACharge(MOCK_GET_CHARGE_URI)).thenReturn(privateChargesGet);
        when(privateChargesGet.execute()).thenReturn(expected);

        final ApiResponse<ChargeApi> response = chargesDataApiService.getACharge(
                MOCK_CONTEXT_ID, MOCK_GET_CHARGE_URI);

        assertThat(response).isEqualTo(expected);
        assertThat(response.getStatusCode()).isEqualTo(200);
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
    @DisplayName("When calling GET charge and an error occurs then throw the appropriate exception based on the error type")
    void getCharge_When_Exception_Are_Thrown(HttpStatus httpStatus,
                                             ApiErrorResponseException exceptionFromApi)
            throws ApiErrorResponseException, URIValidationException {

        final ApiResponse<ChargeApi> expected = new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), Collections.emptyMap());

        when(privateDeltaResourceHandler.getACharge(MOCK_GET_CHARGE_URI)).thenReturn(privateChargesGet);
        when(privateChargesGet.execute()).thenThrow(exceptionFromApi);

        final ApiResponse<ChargeApi> response = chargesDataApiService.getACharge(
                MOCK_CONTEXT_ID, MOCK_GET_CHARGE_URI);

        assertThat(response).isInstanceOf(ApiResponse.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(httpStatus.value());

        verify(mockInternalApiClient, times(1)).getHttpClient();
        verify(mockInternalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .getACharge(MOCK_GET_CHARGE_URI);
        verify(privateChargesGet, times(1))
                .execute();

    }

    @Test
    @DisplayName("When calling GET charge and URLValidation exception occurs then throw the appropriate exception based on the error type")
    void getCharge_When_URIValidationException_Is_Thrown()
            throws ApiErrorResponseException, URIValidationException {

        when(privateDeltaResourceHandler.getACharge(MOCK_GET_CHARGE_URI)).thenReturn(privateChargesGet);
        when(privateChargesGet.execute()).thenThrow(URIValidationException.class);

        assertThrows(RetryableErrorException.class, () -> chargesDataApiService.getACharge(
                MOCK_CONTEXT_ID, MOCK_GET_CHARGE_URI));

        verify(mockInternalApiClient, times(1)).getHttpClient();
        verify(mockInternalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .getACharge(MOCK_GET_CHARGE_URI);
        verify(privateChargesGet, times(1))
                .execute();

    }

    @Test
    @DisplayName("When calling GET charge and APIResponse returns 0 status code")
    void When_GET_Call_With_Exception_With_Status_Code_0_Then_Throw_Appropriate_Exception()
            throws ApiErrorResponseException, URIValidationException {

        when(privateDeltaResourceHandler.getACharge(MOCK_GET_CHARGE_URI)).thenReturn(privateChargesGet);
        when(privateChargesGet.execute()).thenThrow(buildApiErrorResponseCustomException(0));

        assertThrows(RetryableErrorException.class, () -> chargesDataApiService.getACharge(
                MOCK_CONTEXT_ID, MOCK_GET_CHARGE_URI));

        verify(mockInternalApiClient, times(1)).getHttpClient();
        verify(mockInternalApiClient, times(1)).privateDeltaChargeResourceHandler();
        verify(privateDeltaResourceHandler, times(1))
                .getACharge(MOCK_GET_CHARGE_URI);
        verify(privateChargesGet, times(1))
                .execute();

    }


}

package uk.gov.companieshouse.company.metrics.processor;

import org.junit.jupiter.api.Assertions;
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
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.service.ChargesDataApiService;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.company.metrics.util.TestSupport;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyMetricsProcessorTest {

    public static final String CONTEXT_ID = "context_id";
    public static final String MOCK_COMPANY_NUMBER = "02588581";
    public static final String TOPIC = "topic";
    public static final String  PARTITION = "partition";
    public static final String  OFFSET = "offset";
    private static final String MOCK_CHARGE_ID = "MYdKM_YnzAmJ8JtSgVXr61n1bgg";
    private static final String MOCK_GET_CHARGE_URI = String.format("/company/%s/charges/%s",
            MOCK_COMPANY_NUMBER, MOCK_CHARGE_ID);

    private CompanyMetricsProcessor companyMetricsProcessor;

    @Mock
    private CompanyMetricsApiTransformer companyMetricsApiTransformer;

    @Mock
    private CompanyMetricsApiService companyMetricsApiService;

    @Mock
    private ChargesDataApiService chargesDataApiService;

    @Mock
    private Logger logger;
    private TestSupport testSupport;

    @BeforeEach
    void setUp() {
        companyMetricsProcessor = new CompanyMetricsProcessor(companyMetricsApiTransformer, logger,
                companyMetricsApiService, chargesDataApiService);
        testSupport = new TestSupport();
    }

    static Stream<Arguments> testExtractCompanyNumberFromResourceUri() {
        return Stream.of(
                Arguments.of("/company/12345678/charges/--XyzXZanPgGtcE5dTZcrLlk11", "12345678"),
                Arguments.of("/companyabc/12345678/charges/--XyzXZanPgGtcE5dTZcrLlk22", null),
                Arguments.of("/companyz/12345678/xyzzzccharges/--XyzXZanPgGtcE5dTZcrLlk3k", null),
                Arguments.of("/companyabc/12345678/aaxycbccharges/--XyzXZanPgGtcE5dTZcrLlk3k", null),
                Arguments.of("/company/12345611//charges/--XyzxXZanPgGtcE5dTZcrLlk3k", "12345611")
        );
    }

    @ParameterizedTest(name = "{index} ==> {2}: is {0} valid? {1}")
    @MethodSource("testExtractCompanyNumberFromResourceUri")
    void urlPatternTest(String input, String expected) {
        Optional<String> companyNumber = companyMetricsProcessor.extractCompanyNumber(input);
        if (expected != null) {
            assertEquals(expected, companyNumber.get());
        } else {
            Assertions.assertTrue(companyNumber.isEmpty());
        }
    }

    @Test
    @DisplayName("Successfully processes a kafka message containing a ResourceChangedData payload and extract companyNumber")
    void When_ValidResourceChangedDataMessage_Expect_MessageProcessedAndExtractedCompanyNumber() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, false);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(HttpStatus.OK.value(), null, null);

        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        when(companyMetricsApiService.invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER,
                recalculateApiData)).thenReturn(response);

        companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET);

        verify(companyMetricsApiTransformer, times(1)).transform(anyString());
        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiService, times(1))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);
    }

    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, NonRetryableErrorException.class),
                Arguments.of(HttpStatus.NOT_FOUND, RetryableErrorException.class),
                Arguments.of(HttpStatus.UNAUTHORIZED, RetryableErrorException.class),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, RetryableErrorException.class),
                Arguments.of(HttpStatus.BAD_GATEWAY, RetryableErrorException.class),
                Arguments.of(HttpStatus.FORBIDDEN, RetryableErrorException.class),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, RetryableErrorException.class),
                Arguments.of(HttpStatus.GATEWAY_TIMEOUT, RetryableErrorException.class),
                Arguments.of(HttpStatus.TEMPORARY_REDIRECT, RetryableErrorException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    @DisplayName("When calling metrics recalculate an error occurs then throw the appropriate exception based on the error type")
    void postCompanyMetricsRecalculateReturns_Exception_Then_Throw_Appropriate_Exception(
            HttpStatus httpStatus,
            Class<Throwable> exception)
            throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, false);

        final ApiResponse<Void> response = new ApiResponse<>(httpStatus.value(), null, null);
        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(HttpStatus.OK.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        when(companyMetricsApiService.invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);
        assertThrows(exception, () -> companyMetricsProcessor
                .process(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));
        verify(companyMetricsApiTransformer, times(1)).transform(anyString());
        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiService, times(1))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);

    }


    @Test
    @DisplayName("Valid avro message with invalid resource uri, throws Non retryable error")
    void When_ValidMessage_With_InvalidResourceUri() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.INVALID_COMPANY_LINKS_PATH, false);

        assertThrows(NonRetryableErrorException.class, () ->
                companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET));

        verify(logger, times(0)).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(),
                        resourceChangedDataMessage.getPayload())));
        verify(chargesDataApiService, times(0)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiService, times(0)).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                any(MetricsRecalculateApi.class));
    }

    @Test
    @DisplayName("Valid avro message with missing company number, throws Non retryable error")
    void When_ValidMessage_With_MissingCompanyNumber() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH,
                        "", TestSupport.MOCK_CHARGE_ID, false);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);

        assertThrows(NonRetryableErrorException.class, () ->
                companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET));

        verify(logger, times(0)).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(),
                        resourceChangedDataMessage.getPayload())));
        verify(chargesDataApiService, times(0)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiService, times(0)).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                any(MetricsRecalculateApi.class));
    }


    private static Stream<Arguments> provideChargeDataApiExceptionParameters() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, RetryableErrorException.class),
                Arguments.of(HttpStatus.NOT_FOUND, RetryableErrorException.class),
                Arguments.of(HttpStatus.UNAUTHORIZED, RetryableErrorException.class),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, RetryableErrorException.class),
                Arguments.of(HttpStatus.BAD_GATEWAY, RetryableErrorException.class),
                Arguments.of(HttpStatus.FORBIDDEN, RetryableErrorException.class),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, RetryableErrorException.class),
                Arguments.of(HttpStatus.GATEWAY_TIMEOUT, RetryableErrorException.class),
                Arguments.of(HttpStatus.TEMPORARY_REDIRECT, RetryableErrorException.class)
        );
    }


    @ParameterizedTest
    @MethodSource("provideChargeDataApiExceptionParameters")
    @DisplayName("When calling get charge endpoint an error occurs then throw the appropriate exception based on the error type")
    void getChargeApiReturns_Exception_Then_Throw_Appropriate_Exception(
            HttpStatus httpStatus,
            Class<Throwable> exception)
            throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, false);

        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(httpStatus.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        assertThrows(exception, () -> companyMetricsProcessor
                .process(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));

        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiTransformer, times(0)).transform(anyString());
        verify(companyMetricsApiService, times(0))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);

    }

    @Test
    @DisplayName("Successfully processes a kafka message containing a ResourceChangedData delete payload and extract companyNumber")
    void When_ValidResourceChangedData_Delete_Message_Expect_MessageProcessedAndExtractedCompanyNumber() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, true);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(HttpStatus.NOT_FOUND.value(), null, null);

        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        when(companyMetricsApiService.invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER,
                recalculateApiData)).thenReturn(response);

        companyMetricsProcessor.processDelete(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET);

        verify(companyMetricsApiTransformer, times(1)).transform(anyString());
        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiService, times(1))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    @DisplayName("When calling metrics recalculate an error occurs then throw the appropriate exception based on the error type")
    void postCompanyMetricsRecalculateReturns_for_delete_returns_Exception_Then_Throw_Appropriate_Exception(
            HttpStatus httpStatus,
            Class<Throwable> exception)
            throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, true);

        final ApiResponse<Void> response = new ApiResponse<>(httpStatus.value(), null, null);
        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(HttpStatus.NOT_FOUND.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        when(companyMetricsApiService.invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);
        assertThrows(exception, () -> companyMetricsProcessor
                .processDelete(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));
        verify(companyMetricsApiTransformer, times(1)).transform(anyString());
        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiService, times(1))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);

    }

    private static Stream<Arguments> provideChargeDataApiDeleteExceptionParameters() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, RetryableErrorException.class),
                Arguments.of(HttpStatus.OK, RetryableErrorException.class),
                Arguments.of(HttpStatus.UNAUTHORIZED, RetryableErrorException.class),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, RetryableErrorException.class),
                Arguments.of(HttpStatus.BAD_GATEWAY, RetryableErrorException.class),
                Arguments.of(HttpStatus.FORBIDDEN, RetryableErrorException.class),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, RetryableErrorException.class),
                Arguments.of(HttpStatus.GATEWAY_TIMEOUT, RetryableErrorException.class),
                Arguments.of(HttpStatus.TEMPORARY_REDIRECT, RetryableErrorException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideChargeDataApiDeleteExceptionParameters")
    @DisplayName("When calling get charge endpoint for delete message an error occurs then throw the appropriate exception based on the error type")
    void getChargeApiReturns_For_delete_Exception_Then_Throw_Appropriate_Exception(
            HttpStatus httpStatus,
            Class<Throwable> exception)
            throws IOException {

        Message<ResourceChangedData> mockResourceChangedMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, false);

        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(httpStatus.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        final String exceptionMessage = assertThrows(exception, () -> companyMetricsProcessor
                .processDelete(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET)).getMessage();
        assertEquals("Charge details found!", exceptionMessage);

        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiTransformer, times(0)).transform(anyString());
        verify(companyMetricsApiService, times(0))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);

    }

    @Test
    @DisplayName("When calling get charge endpoint for delete message then a RetryableErrorException is thrown if a 404 response is received")
    void getChargeApiReturns_404_for_delete_then_RetryableErrorException_is_thrown() throws IOException {

        Message<ResourceChangedData> mockResourceChangedMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, false);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.NOT_FOUND.value(), null, null);
        final ApiResponse<ChargeApi> chargeDataApiResponse =
                new ApiResponse<>(HttpStatus.NOT_FOUND.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(chargesDataApiService.getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI))
                .thenReturn(chargeDataApiResponse);
        when(companyMetricsApiService.invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER,null)).thenReturn(response);
        final String exceptionMessage = assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor
                .processDelete(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET)).getMessage();
        assertEquals("Non-Successful response received from company-metrics-api", exceptionMessage);

        verify(chargesDataApiService, times(1)).getACharge(CONTEXT_ID, MOCK_GET_CHARGE_URI);
        verify(companyMetricsApiTransformer, times(1)).transform(anyString());
        verify(companyMetricsApiService, times(0))
                .invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER, recalculateApiData);

    }
}

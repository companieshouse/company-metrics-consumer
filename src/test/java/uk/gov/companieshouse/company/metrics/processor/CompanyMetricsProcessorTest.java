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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.util.TestSupport;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
public class CompanyMetricsProcessorTest {

    public static final String CONTEXT_ID = "context_id";
    public static final String MOCK_COMPANY_NUMBER = "02588581";
    public static final String TOPIC = "topic";
    public static final String  PARTITION = "partition";
    public static final String  OFFSET = "offset";

    private CompanyMetricsProcessor companyMetricsProcessor;

    @Mock
    private CompanyMetricsApiTransformer companyMetricsApiTransformer;

    @Mock
    private CompanyMetricsApiService companyMetricsApiService;

    @Mock
    private Logger logger;
    private TestSupport testSupport;

    @BeforeEach
    void setUp() {
        companyMetricsProcessor = new CompanyMetricsProcessor(companyMetricsApiTransformer, logger, companyMetricsApiService);
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
    public void urlPatternTest(String input, String expected) {
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

        Message<ResourceChangedData> resourceChangedDataMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi(CONTEXT_ID, MOCK_COMPANY_NUMBER,
                recalculateApiData)).thenReturn(response);

        companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET);

        verify(companyMetricsApiService).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                Mockito.any(MetricsRecalculateApi.class));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns Bad Request 400, non-retryable error")
    void postCompanyMetricsRecalculateReturnsBadRequest_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);

        assertThrows(NonRetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns unauthorised 401, retryable error")
    void postCompanyMetricsRecalculateReturnsUnauthorized_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns forbidden 403, retryable error")
    void postCompanyMetricsRecalculateReturnsForbidden_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.FORBIDDEN.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns internal server error 500, retryable error")
    void postCompanyMetricsRecalculateReturnsInternalServerError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns service unavailable 503, retryable error")
    void postCompanyMetricsRecalcReturnsInternalServerError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.SERVICE_UNAVAILABLE.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns bad gateway 502, retryable error")
    void postCompanyMetricsRecalcReturnsBadGatewayError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.BAD_GATEWAY.value(), null, null);
        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns gateway timeout 504, retryable error")
    void postCompanyMetricsRecalcReturnsGatewayTimeoutError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.GATEWAY_TIMEOUT.value(),
                null, null);

        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);
        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns temporary redirect 307, retryable error")
    void postCompanyMetricsRecalcReturnsTemporaryRedirect_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.TEMPORARY_REDIRECT.value(),
                null, null);

        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenReturn(response);
        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("Valid avro message with invalid resource uri, throws Non retryable error")
    void When_ValidMessage_With_InvalidResourceUri() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.INVALID_COMPANY_LINKS_PATH);
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);

        assertThrows(NonRetryableErrorException.class, () ->
                companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET));

        verify(logger, times(0)).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(),
                        resourceChangedDataMessage.getPayload())));

        verify(companyMetricsApiService, times(0)).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                Mockito.any(MetricsRecalculateApi.class));
    }

    @Test
    @DisplayName("Valid avro message with missing company number, throws Non retryable error")
    void When_ValidMessage_With_MissingCompanyNumber() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH, "");
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);

        assertThrows(NonRetryableErrorException.class, () ->
                companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET));

        verify(logger, times(0)).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(),
                        resourceChangedDataMessage.getPayload())));

        verify(companyMetricsApiService, times(0)).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                Mockito.any(MetricsRecalculateApi.class));
    }

    @Test
    @DisplayName("Valid avro message but backend api service, throws 400 bad request")
    void When_ValidMessage_but_backendApiService_throws_400_error() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ResponseStatusException responseStatusException = testSupport.getResponseStatusException(400);

        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER,
                recalculateApiData))
                .thenThrow(responseStatusException);

        assertThrows(NonRetryableErrorException.class, () ->
                companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET));

        verify(companyMetricsApiService, times(1)).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                Mockito.any(MetricsRecalculateApi.class));
    }

    @Test
    @DisplayName("Valid avro message but backend api service, throws 503 bad request")
    void When_ValidMessage_but_backendApiService_throws_503_error() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage =
                testSupport.createResourceChangedMessage(TestSupport.VALID_COMPANY_LINKS_PATH);

        final ResponseStatusException responseStatusException = testSupport.getResponseStatusException(503);

        MetricsRecalculateApi recalculateApiData = testSupport.createMetricsRecalculateApiData();
        when(companyMetricsApiTransformer.transform(anyString())).thenReturn(recalculateApiData);
        when(companyMetricsApiService.invokeMetricsPostApi("context_id", MOCK_COMPANY_NUMBER, recalculateApiData))
                .thenThrow(responseStatusException);

        assertThrows(RetryableErrorException.class, () ->
                companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET));

        verify(companyMetricsApiService, times(1)).invokeMetricsPostApi(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                Mockito.any(MetricsRecalculateApi.class));
    }


}

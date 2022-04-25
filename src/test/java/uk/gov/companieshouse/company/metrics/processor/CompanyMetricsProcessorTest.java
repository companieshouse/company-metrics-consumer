package uk.gov.companieshouse.company.metrics.processor;

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
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.model.TestData;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private CompanyMetricsApiTransformer transformer;

    @Mock
    private CompanyMetricsApiService companyMetricsApiService;

    @Mock
    private Logger logger;
    private TestData testData;

    @BeforeEach
    void setUp() {
        companyMetricsProcessor = new CompanyMetricsProcessor(transformer, logger, companyMetricsApiService);
        testData = new TestData();
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
        String companyNumber = companyMetricsProcessor.extractCompanyNumber(input);
        assertEquals(expected, companyNumber);
    }

    @Test
    @DisplayName("Successfully processes a kafka message containing a ResourceChangedData payload and extract companyNumber")
    void When_ValidResourceChangedDataMessage_Expect_MessageProcessedAndExtractedCompanyNumber() throws IOException {

        Message<ResourceChangedData> resourceChangedDataMessage = testData.createResourceChangedMessage();
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics(CONTEXT_ID, MOCK_COMPANY_NUMBER,
                testData.createMetricsRecalculateApiData())).thenReturn(response);

        companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), TOPIC, PARTITION, OFFSET);

        verify(logger, atLeastOnce()).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(),
                        resourceChangedDataMessage.getPayload())));

        verify(companyMetricsApiService).postCompanyMetrics(eq(CONTEXT_ID), eq(MOCK_COMPANY_NUMBER),
                Mockito.any(MetricsRecalculateApi.class));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns Bad Request 400, non-retryable error")
    void postCompanyMetricsRecalculateReturnsBadRequest_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);

        assertThrows(NonRetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns unauthorised 401, retryable error")
    void postCompanyMetricsRecalculateReturnsUnauthorized_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns forbidden 403, retryable error")
    void postCompanyMetricsRecalculateReturnsForbidden_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.FORBIDDEN.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(),TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns internal server error 500, retryable error")
    void postCompanyMetricsRecalculateReturnsInternalServerError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns service unavailable 503, retryable error")
    void postCompanyMetricsRecalcReturnsInternalServerError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.SERVICE_UNAVAILABLE.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns bad gateway 502, retryable error")
    void postCompanyMetricsRecalcReturnsBadGatewayError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();

        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.BAD_GATEWAY.value(), null, null);
        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);

        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns gateway timeout 504, retryable error")
    void postCompanyMetricsRecalcReturnsGatewayTimeoutError_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.GATEWAY_TIMEOUT.value(),
                null, null);

        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);
        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

    @Test
    @DisplayName("POST company metrics recalculate returns temporary redirect 307, retryable error")
    void postCompanyMetricsRecalcReturnsTemporaryRedirect_then_nonRetryableError() throws IOException {
        Message<ResourceChangedData> mockResourceChangedMessage = testData.createResourceChangedMessage();
        final ApiResponse<Void> response = new ApiResponse<>(HttpStatus.TEMPORARY_REDIRECT.value(),
                null, null);

        when(companyMetricsApiService.postCompanyMetrics("context_id", MOCK_COMPANY_NUMBER, testData.createMetricsRecalculateApiData()))
                .thenReturn(response);
        assertThrows(RetryableErrorException.class, () -> companyMetricsProcessor.process(mockResourceChangedMessage.getPayload(), TOPIC, PARTITION, OFFSET));
    }

}

package uk.gov.companieshouse.company.metrics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.metrics.PrivateCompanyMetricsUpsertHandler;
import uk.gov.companieshouse.api.handler.metrics.request.PrivateCompanyMetricsUpsert;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
    private InternalApiClient apiClient;

    @Mock
    private PrivateCompanyMetricsUpsertHandler privateCompanyMetricsUpsertHandler;

    @Mock
    private PrivateCompanyMetricsUpsert privateCompanyMetricsUpsert;


    @BeforeEach
    void setup() {
        companyMetricsApiService = spy(new CompanyMetricsApiService(logger));
        when(companyMetricsApiService.getApiClient(MOCK_CONTEXT_ID)).thenReturn(apiClient);
        when(apiClient.privateCompanyMetricsUpsertHandler()).thenReturn(privateCompanyMetricsUpsertHandler);
    }

    @Test
    @DisplayName("Successfully send a POST request with a company metrics")
    void postCompanyMetrics() throws ApiErrorResponseException, URIValidationException {

        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);

        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        internalData.setUpdatedAt(OffsetDateTime.now());
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);

        final ApiResponse<Void> expected = new ApiResponse<>(
                HttpStatus.OK.value(), Collections.emptyMap());

        when(privateCompanyMetricsUpsertHandler.postCompanyMetrics(MOCK_COMPANY_METRICS_RECALCULATE_URI, metricsRecalculateApi)).thenReturn(privateCompanyMetricsUpsert);
        when(privateCompanyMetricsUpsert.execute()).thenReturn(expected);

        final ApiResponse<Void> response = companyMetricsApiService.postCompanyMetrics(
                MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, metricsRecalculateApi);

        assertThat(response).isEqualTo(expected);
    }
}

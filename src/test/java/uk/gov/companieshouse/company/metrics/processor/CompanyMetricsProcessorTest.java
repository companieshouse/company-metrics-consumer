package uk.gov.companieshouse.company.metrics.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.company.metrics.producer.CompanyMetricsProducer;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.delta.ChsDelta;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
public class CompanyMetricsProcessorTest {

    private CompanyMetricsProcessor companyMetricsProcessor;

    @Mock
    private CompanyMetricsProducer companyMetricsProducer;

    @Mock
    private CompanyMetricsApiTransformer transformer;

    @BeforeEach
    void setUp() {
        companyMetricsProcessor = new CompanyMetricsProcessor(companyMetricsProducer, transformer);
    }

    @Test
    @DisplayName("Transforms a kafka message containing a ChsDelta payload into an CompanyMetrics")
    void When_ValidChsDeltaMessage_Expect_ValidCompanyMetricsMapping() throws IOException {
        Message<ChsDelta> mockChsDeltaMessage = createChsDeltaMessage();
        // FIXME - When processor and data model is ready please uncomment it and make required changes.

        /*Object expectedCompanyMetrics = createCompanyMetrics();
        when(transformer.transform(expectedCompanyMetrics)).thenCallRealMethod();
        companyMetricsProcessor.processMetrics(mockChsDeltaMessage);
        verify(transformer).transform(expectedCompanyMetrics);*/
    }

    private Message<ChsDelta> createChsDeltaMessage() throws IOException {
        InputStreamReader exampleCompanyMetricsJsonPayload = new InputStreamReader(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream("company-metrics-example.json")));
        String companyMetricsData = FileCopyUtils.copyToString(exampleCompanyMetricsJsonPayload);

        ChsDelta mockChsDelta = ChsDelta.newBuilder()
                .setData(companyMetricsData)
                .setContextId("context_id")
                .setAttempt(1)
                .build();

        return MessageBuilder
                .withPayload(mockChsDelta)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "test")
                .setHeader("COMPANY_METRICS_RETRY_COUNT", 1)
                .build();
    }

    private Object createCompanyMetrics() {
        // FIXME - When Company Metrics Model is ready set an object with data.
        return new Object();
    }
}

package uk.gov.companieshouse.company.metrics.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.company.metrics.model.TestData;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CompanyMetricsProcessorTest {

    private CompanyMetricsProcessor companyMetricsProcessor;

    @Mock
    private CompanyMetricsApiTransformer transformer;

    @Mock
    private CompanyMetricsApiService companyMetricsApiService;

    @Mock
    private Logger logger;
    private TestData testData;
    private String topic;
    private String partition;
    private String offset;

    @BeforeEach
    void setUp() {
        companyMetricsProcessor = new CompanyMetricsProcessor(transformer,
                companyMetricsApiService, logger);
        testData = new TestData();
    }

    @Test
    @DisplayName("Successfully processes a kafka message containing a ResourceChangedData payload and extract companyNumber")
    void When_ValidResourceChangedDataMessage_Expect_MessageProcessedAndExtractedCompanyNumber() throws IOException {
        Message<ResourceChangedData> resourceChangedDataMessage = testData.createResourceChangedMessage();
        companyMetricsProcessor.process(resourceChangedDataMessage.getPayload(), topic, partition, offset);

        verify(logger, atLeastOnce()).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(), resourceChangedDataMessage.getPayload())));
    }

}

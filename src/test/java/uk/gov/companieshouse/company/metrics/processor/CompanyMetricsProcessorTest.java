package uk.gov.companieshouse.company.metrics.processor;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.company.metrics.model.TestData;
import uk.gov.companieshouse.company.metrics.producer.CompanyMetricsProducer;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CompanyMetricsProcessorTest {

    private CompanyMetricsProcessor companyMetricsProcessor;

    @Mock
    private CompanyMetricsProducer companyMetricsProducer;

    @Mock
    private CompanyMetricsApiTransformer transformer;

    @Mock
    private Logger logger;
    private TestData testData;

    @BeforeEach
    void setUp() {
        companyMetricsProcessor = new CompanyMetricsProcessor(companyMetricsProducer, transformer, logger);
        testData = new TestData();
    }

    @Test
    @DisplayName("Successfully processes a kafka message containing a ResourceChangedData payload and extract companyNumber")
    void When_ValidResourceChangedDataMessage_Expect_MessageProcessedAndExtractedCompanyNumber() throws IOException {
        Message<ResourceChangedData> resourceChangedDataMessage = testData.createResourceChangedMessage();
        companyMetricsProcessor.process(resourceChangedDataMessage);

        verify(logger, atLeastOnce()).trace((
                String.format("Company number %s extracted from"
                                + " ResourceURI %s the payload is %s ", "02588581",
                        resourceChangedDataMessage.getPayload().getResourceUri(), resourceChangedDataMessage.getPayload())));
    }

}

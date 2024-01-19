package uk.gov.companieshouse.company.metrics.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NullMetricsClientTest {

    private static final String COMPANY_NUMBER = "01203396";
    private static final String UPDATED_BY = "updatedBy";
    private static final String RESOURCE_URI = String.format("/company/%s/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg", COMPANY_NUMBER);
    private static final String CONTEXT_ID = "context_id";

    @Mock
    private Logger logger;

    @InjectMocks
    private NullMetricsClient client;

    @Test
    void testThrowNonRetryableExceptionIfPostMetricsInvoked() {
        // given
        //when
        Executable actual = () -> client.postMetrics(COMPANY_NUMBER, UPDATED_BY, RESOURCE_URI, CONTEXT_ID);

        //then
        Exception exception = assertThrows(NonRetryableErrorException.class, actual);
        String expectedMessage = "Invalid delta type and/or event type for company number 01203396";
        assertEquals(expectedMessage, exception.getMessage());
        verify(logger).error(expectedMessage);

    }
}

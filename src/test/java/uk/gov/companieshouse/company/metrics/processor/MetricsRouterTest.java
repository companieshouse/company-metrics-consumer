package uk.gov.companieshouse.company.metrics.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.company.metrics.service.CompanyNumberExtractable;
import uk.gov.companieshouse.company.metrics.service.MetricsClientFactory;
import uk.gov.companieshouse.company.metrics.service.ChangedChargesClient;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsRouterTest {

    @Mock
    private CompanyNumberExtractable extractor;

    @Mock
    private MetricsClientFactory factory;

    @Mock
    private ChangedChargesClient changedChargesClient;

    private MetricsRouter router;

    @Mock
    private ResourceChange message;

    @Mock
    private ResourceChangedData data;

    @Mock
    private EventRecord event;

    @BeforeEach
    void setup() {
        router = new MetricsRouter(extractor, factory);
    }

    @Test
    @DisplayName("Route should successfully route changed events to the recalculate charges client service")
    void routeChangedCharges() {
        // given
        when(message.getData()).thenReturn(data);
        when(data.getEvent()).thenReturn(event);
        when(event.getType()).thenReturn("changed");
        when(data.getResourceUri()).thenReturn("/company/01203396/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg");
        when(data.getContextId()).thenReturn("context_id");
        when(extractor.extractCompanyNumber(any())).thenReturn("01203396");
        when(factory.getMetricsClient(any(), any())).thenReturn(changedChargesClient);

        // when
        router.route(message, "deltaType", "updatedBy");

        // then
        verify(extractor).extractCompanyNumber("/company/01203396/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg");
        verify(changedChargesClient).postMetrics("01203396", "updatedBy", "/company/01203396/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg", "context_id");
    }

    @Test
    @DisplayName("Route should successfully route changed events to the recalculate charges client service")
    void routeDeletedCharges() {
        // given
        when(message.getData()).thenReturn(data);
        when(data.getEvent()).thenReturn(event);
        when(event.getType()).thenReturn("deleted");
        when(data.getResourceUri()).thenReturn("/company/01203396/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg");
        when(data.getContextId()).thenReturn("context_id");
        when(extractor.extractCompanyNumber(any())).thenReturn("01203396");
        when(factory.getMetricsClient(any(), any())).thenReturn(changedChargesClient);

        // when
        router.route(message, "deltaType", "updatedBy");

        // then
        verify(extractor).extractCompanyNumber("/company/01203396/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg");
        verify(changedChargesClient).postMetrics("01203396", "updatedBy", "/company/01203396/charges/MYdKM_YnzAmJ8JtSgVXr61n1bgg", "context_id");
    }

}


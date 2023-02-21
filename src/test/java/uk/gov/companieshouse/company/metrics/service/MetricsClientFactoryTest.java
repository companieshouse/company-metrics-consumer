package uk.gov.companieshouse.company.metrics.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MetricsClientFactoryTest {

    @Autowired
    private MetricsClientFactory factory;

    @Test
    @DisplayName("Metrics factory correctly returns a changed charges client client")
    void getRecalculateChargesClient() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("charges", "changed");

        // then
        assertTrue(metricsClient instanceof ChangedChargesClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns a deleted charges client client")
    void getDeleteChargesClient() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("charges", "deleted");

        // then
        assertTrue(metricsClient instanceof DeletedChargesClient);
    }



    @Test
    @DisplayName("Metrics factory correctly returns a null metrics client when deltaType mismatch")
    void getDefaultMetricsClientDeltaType() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("deltaType", "changed");

        // then
        assertTrue(metricsClient instanceof NullMetricsClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns a null metrics client when eventType mismatch")
    void getDefaultMetricsClientEventType() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("exemptions", "eventType");

        // then
        assertTrue(metricsClient instanceof NullMetricsClient);
    }
}
package uk.gov.companieshouse.company.metrics.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.company.metrics.util.TestConfig;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test_main.properties")
@Import(TestConfig.class)
@ActiveProfiles("test_main")
class MetricsClientFactoryTest {

    @Autowired
    private MetricsClientFactory factory;

    @Test
    @DisplayName("Metrics factory correctly returns a changed charges client")
    void getRecalculateChargesClient() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("charges", "changed");

        // then
        assertTrue(metricsClient instanceof ChangedChargesClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns a deleted charges client")
    void getDeleteChargesClient() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("charges", "deleted");

        // then
        assertTrue(metricsClient instanceof DeletedChargesClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns an appointments client with a changed event type")
    void getAppointmentsClientWhenChangedEventType() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("officers", "changed");

        // then
        assertTrue(metricsClient instanceof AppointmentsClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns an appointments client with a deleted event type")
    void getAppointmentsClientWhenDeletedEventType() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("officers", "deleted");

        // then
        assertTrue(metricsClient instanceof AppointmentsClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns a Pscs client with a changed event type")
    void getPSCsClientWhenChangedEventType() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("pscs", "changed");

        // then
        assertTrue(metricsClient instanceof PscsClient);
    }

    @Test
    @DisplayName("Metrics factory correctly returns a Pscs client with a deleted event type")
    void getPSCsClientWhenDeletedEventType() {
        // given
        // when
        MetricsClient metricsClient = factory.getMetricsClient("pscs", "deleted");

        // then
        assertTrue(metricsClient instanceof PscsClient);
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
package uk.gov.companieshouse.company.metrics.transformer;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyMetricsApiTransformerTest {

    private static final String MOCK_TOPIC = "topic";
    private static final String MOCK_PARTITION = "partition";
    private static final String MOCK_OFFSET = "offset";

    private final CompanyMetricsApiTransformer transformer = new CompanyMetricsApiTransformer();

    @Test
    void testSuccessfulTransformForMortgageIsTrue() {
        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);
        MetricsRecalculateApi expected = new MetricsRecalculateApi()
                .mortgage(true)
                .appointments(false)
                .personsWithSignificantControl(false)
                .registers(false)
                .internalData(new InternalData()
                        .updatedBy(updatedBy));

        // when
        MetricsRecalculateApi actual = transformer.transform(updatedBy, true, false, false, false);

        // then
        assertEquals(expected, actual);
    }

    @Test
    void testSuccessfulTransformForAppointmentsIsTrue() {
        // given
        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);
        MetricsRecalculateApi expected = new MetricsRecalculateApi()
                .mortgage(false)
                .appointments(true)
                .personsWithSignificantControl(false)
                .registers(false)
                .internalData(new InternalData()
                        .updatedBy(updatedBy));

        // when
        MetricsRecalculateApi actual = transformer.transform(updatedBy, false, true, false, false);

        // then
        assertEquals(expected, actual);
    }

    @Test
    void testSuccessfulTransformForRegistersIsTrue() {
        // given
        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);
        MetricsRecalculateApi expected = new MetricsRecalculateApi()
                .mortgage(false)
                .appointments(false)
                .personsWithSignificantControl(false)
                .registers(true)
                .internalData(new InternalData()
                        .updatedBy(updatedBy));

        // when
        MetricsRecalculateApi actual = transformer.transform(updatedBy, false, false, false, true);

        // then
        assertEquals(expected, actual);
    }

}

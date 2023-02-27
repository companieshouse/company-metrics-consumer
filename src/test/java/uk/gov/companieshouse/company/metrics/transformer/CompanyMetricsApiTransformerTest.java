package uk.gov.companieshouse.company.metrics.transformer;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyMetricsApiTransformerTest {

    private static final String MOCK_TOPIC = "topic";
    private static final String MOCK_PARTITION = "partition";
    private static final String MOCK_OFFSET = "offset";

    private final CompanyMetricsApiTransformer transformer = new CompanyMetricsApiTransformer();

    @Test
    void testTransformSuccessfully() {
        final String updatedBy = String.format("%s-%s-%s", MOCK_TOPIC, MOCK_PARTITION, MOCK_OFFSET);
        MetricsRecalculateApi expectedApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);

        expectedApi.setMortgage(Boolean.TRUE);
        expectedApi.setAppointments(Boolean.FALSE);
        expectedApi.setPersonsWithSignificantControl(Boolean.FALSE);
        expectedApi.setInternalData(internalData);

        assertThat(transformer.transform(updatedBy)).isEqualTo(expectedApi);
    }

}

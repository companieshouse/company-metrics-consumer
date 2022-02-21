package uk.gov.companieshouse.company.metrics.transformer;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.delta.InsolvencyDelta;

import static org.assertj.core.api.Assertions.assertThat;

public class CompanyMetricsApiTransformerTest {

    private final CompanyMetricsApiTransformer transformer = new CompanyMetricsApiTransformer();

    @Test
    public void transformSuccessfully() {
        final Object input = new Object(); // FIXME - CompanyMetrics Model to be used
        assertThat(transformer.transform(input)).isEqualTo(input.toString());
    }

}

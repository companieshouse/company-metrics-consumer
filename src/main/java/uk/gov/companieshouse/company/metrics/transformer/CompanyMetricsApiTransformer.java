package uk.gov.companieshouse.company.metrics.transformer;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;

@Component
public class CompanyMetricsApiTransformer {

    /**
     * Prepare MetricsRecalculateApi object.
     */
    public MetricsRecalculateApi transform(String updatedBy) {
        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();

        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);

        return metricsRecalculateApi;
    }
}

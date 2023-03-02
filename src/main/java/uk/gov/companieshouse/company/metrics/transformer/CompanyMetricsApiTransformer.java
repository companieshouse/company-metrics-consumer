package uk.gov.companieshouse.company.metrics.transformer;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;

@Component
public class CompanyMetricsApiTransformer {

    /**
     * Prepare MetricsRecalculateApi object.
     */
    public MetricsRecalculateApi transform(String updatedBy,
                                           Boolean isMortgage,
                                           Boolean isAppointment,
                                           Boolean isPsc) {
        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();

        InternalData internalData = new InternalData();
        internalData.setUpdatedBy(updatedBy);
        metricsRecalculateApi.setMortgage(isMortgage);
        metricsRecalculateApi.setAppointments(isAppointment);
        metricsRecalculateApi.setPersonsWithSignificantControl(isPsc);
        metricsRecalculateApi.setInternalData(internalData);

        return metricsRecalculateApi;
    }
}

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
                                           boolean isMortgage,
                                           boolean isAppointment,
                                           boolean isPsc,
                                           boolean isRegister) {
        return new MetricsRecalculateApi()
                .mortgage(isMortgage)
                .appointments(isAppointment)
                .personsWithSignificantControl(isPsc)
                .registers(isRegister)
                .internalData(new InternalData()
                        .updatedBy(updatedBy));
    }
}

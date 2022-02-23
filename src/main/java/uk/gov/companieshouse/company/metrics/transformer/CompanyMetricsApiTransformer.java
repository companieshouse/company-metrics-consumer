package uk.gov.companieshouse.company.metrics.transformer;

import org.springframework.stereotype.Component;

@Component
public class CompanyMetricsApiTransformer {

    // FIXME - CompanyMetrics Model to be referred from private SDK when its ready
    public String transform(Object companyMetrics) {
        //TODO: Use mapStruct to transform json object to Open API generated object
        return companyMetrics.toString();
    }
}

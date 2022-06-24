package uk.gov.companieshouse.company.metrics.processor;

@FunctionalInterface
public interface MetricsProcessor<T, U, V, W> {

    void apply(T contextId, U uri, V companyNumber, W updatedBy);
}

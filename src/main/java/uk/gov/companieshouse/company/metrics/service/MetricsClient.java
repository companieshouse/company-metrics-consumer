package uk.gov.companieshouse.company.metrics.service;

public interface MetricsClient {
    void postMetrics(String companyNumber, String updatedBy, String resourceUri, String contextId);
}

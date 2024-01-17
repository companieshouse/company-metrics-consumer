package uk.gov.companieshouse.company.metrics.service;

import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

public interface ResponseHandler {
    void handle(String companyNumber, String deltaType,
                IllegalArgumentException exception, String contextId);

    void handle(String companyNumber, String deltaType,
                ApiErrorResponseException exception, String contextId);

    void handle(String companyNumber, String deltaType,
                URIValidationException exception, String contextId);
}

package uk.gov.companieshouse.company.metrics.service;

import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

public interface ResponseHandler {
    void handle(String companyNumber, String deltaType, IllegalArgumentException exception);

    void handle(String companyNumber, String deltaType, ApiErrorResponseException exception);

    void handle(String companyNumber, String deltaType, URIValidationException exception);
}

package uk.gov.companieshouse.company.metrics.exception;

public class RetryableException extends RuntimeException {
    public RetryableException(String message) {
        super(message);
    }
}


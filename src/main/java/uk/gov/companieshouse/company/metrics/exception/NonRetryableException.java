package uk.gov.companieshouse.company.metrics.exception;

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) {
        super(message);
    }
}


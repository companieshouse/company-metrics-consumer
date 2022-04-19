package uk.gov.companieshouse.company.metrics.exception;

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) {
        super(message);
    }

    public NonRetryableException(String message, Exception exception) {
        super(message, exception);
    }

    public NonRetryableException(Exception exception) {
        super(exception);
    }
}


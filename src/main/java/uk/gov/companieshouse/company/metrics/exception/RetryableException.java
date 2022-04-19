package uk.gov.companieshouse.company.metrics.exception;

public class RetryableException extends RuntimeException {
    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Exception exception) {
        super(message, exception);
    }

    public RetryableException(Exception exception) {
        super(exception);
    }
}


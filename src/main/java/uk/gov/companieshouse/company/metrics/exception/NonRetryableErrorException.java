package uk.gov.companieshouse.company.metrics.exception;

public class NonRetryableErrorException extends RuntimeException {
    public NonRetryableErrorException(String message) {
        super(message);
    }

    public NonRetryableErrorException(String message, Exception exception) {
        super(message, exception);
    }

    public NonRetryableErrorException(Exception exception) {
        super(exception);
    }
}


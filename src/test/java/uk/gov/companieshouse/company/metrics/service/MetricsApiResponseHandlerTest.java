package uk.gov.companieshouse.company.metrics.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsApiResponseHandlerTest {

    private static final String FAILED_MSG = "Failed recalculating %s for company %s with context id %s";
    private static final String ERROR_MSG = "Error %s recalculating %s for company %s with context id %s";
    private static final String COMPANY_NUMBER = "12345678";
    private static final String APPOINTMENTS_DELTA_TYPE = "appointments";
    private static final String CONTEXT_ID = "1234";

    @Mock
    private Logger logger;

    @Mock
    private IllegalArgumentException illegalArgumentException;

    @Mock
    private Throwable throwable;

    @InjectMocks
    private MetricsApiResponseHandler metricsApiResponseHandler;

    @Test
    void testHandleUriValidationException() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER, CONTEXT_ID);

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new URIValidationException(any()), CONTEXT_ID);

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).error(message);
    }

    @Test
    void testHandleIllegalArgumentException() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER, CONTEXT_ID);
        String causeMessage = "cause message";
        when(illegalArgumentException.getCause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(causeMessage);

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException, CONTEXT_ID);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).info(message + "; " + causeMessage);
    }

    @Test
    void testHandleIllegalArgumentExceptionWhenCauseIsNull() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER, CONTEXT_ID);

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException, CONTEXT_ID);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).info(message);
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode500() {
        // given
        final int statusCodeInternalServerError = 500;
        String message = String.format(ERROR_MSG, statusCodeInternalServerError, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER, CONTEXT_ID);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeInternalServerError, "", new HttpHeaders());

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder), CONTEXT_ID);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).info(message);
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode404() {
        // given
        final int statusCodeNotFound = 404;
        String message = String.format(ERROR_MSG, statusCodeNotFound, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER, CONTEXT_ID);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeNotFound, "", new HttpHeaders());

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder), CONTEXT_ID);

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).error(message);
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCodeIsNot404Or5xx() {
        // given
        final int statusCodeForbidden = 403;
        String message = String.format(ERROR_MSG, statusCodeForbidden, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER, CONTEXT_ID);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeForbidden, "", new HttpHeaders());

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder), CONTEXT_ID);

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).error(message);
    }
}

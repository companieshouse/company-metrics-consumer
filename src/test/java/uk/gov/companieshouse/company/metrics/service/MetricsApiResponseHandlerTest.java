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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsApiResponseHandlerTest {

    private static final String FAILED_MSG = "Failed recalculating %s for company %s";
    private static final String ERROR_MSG = "Error %s recalculating %s for company %s";
    private static final String COMPANY_NUMBER = "12345678";
    private static final String APPOINTMENTS_DELTA_TYPE = "appointments";

    @Mock
    private Logger logger;

    @Mock
    private IllegalArgumentException illegalArgumentException;

    @Mock
    private Throwable throwable;

    @InjectMocks
    private MetricsApiResponseHandlerImpl responseHandler;

    @Test
    void testHandleUriValidationException() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);

        // when
        Executable actual = () -> responseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new URIValidationException(any()));

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).error(message);
    }

    @Test
    void testHandleIllegalArgumentException() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        String causeMessage = "cause message";
        when(illegalArgumentException.getCause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(causeMessage);

        // when
        Executable actual = () -> responseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).info(message + "; " + causeMessage);
    }

    @Test
    void testHandleIllegalArgumentExceptionWhenCauseIsNull() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);

        // when
        Executable actual = () -> responseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).info(message);
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode500() {
        // given
        int statusCode = 500;
        String message = String.format(ERROR_MSG, statusCode, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCode, "", new HttpHeaders());

        // when
        Executable actual = () -> responseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder));

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).info(message);
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode404() {
        // given
        int statusCode = 404;
        String message = String.format(ERROR_MSG, statusCode, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCode, "", new HttpHeaders());

        // when
        Executable actual = () -> responseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder));

        // then
        assertDoesNotThrow(actual);
        verify(logger).info(message);
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCodeIsNot404Or5xx() {
        // given
        int statusCode = 403;
        String message = String.format(ERROR_MSG, statusCode, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCode, "", new HttpHeaders());

        // when
        Executable actual = () -> responseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder));

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
        verify(logger).error(message);
    }
}
package uk.gov.companieshouse.company.metrics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class MetricsApiResponseHandlerTest {

    private static final String FAILED_MSG = "Failed recalculating %s for company %s";
    private static final String ERROR_MSG = "HTTP response code %s  when recalculating %s for company %s";
    private static final String API_INFO_RESPONSE_MSG = "Call to API failed, status code: %d. %s";
    private static final String COMPANY_NUMBER = "12345678";
    private static final String APPOINTMENTS_DELTA_TYPE = "appointments";

    @Mock
    private IllegalArgumentException illegalArgumentException;

    @Mock
    private Throwable throwable;

    @InjectMocks
    private MetricsApiResponseHandler metricsApiResponseHandler;

    @Test
    void testHandleUriValidationException() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE,
                new URIValidationException("some message"));

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testHandleIllegalArgumentException() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        String causeMessage = "cause message";
        when(illegalArgumentException.getCause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(causeMessage);

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testHandleIllegalArgumentExceptionWhenCauseIsNull() {
        // given
        String message = String.format(FAILED_MSG, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, illegalArgumentException);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode500() {
        // given
        final int statusCodeInternalServerError = 500;
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeInternalServerError, "", new HttpHeaders());
        ApiErrorResponseException exception = new ApiErrorResponseException(builder);
        String message = String.format(API_INFO_RESPONSE_MSG, statusCodeInternalServerError, Arrays.toString(exception.getStackTrace()));

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, exception);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCodeIsNot400Or409() {
        // given
        final int statusCodeForbidden = 403;
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeForbidden, "", new HttpHeaders());
        ApiErrorResponseException exception = new ApiErrorResponseException(builder);
        String message = String.format(API_INFO_RESPONSE_MSG, statusCodeForbidden, Arrays.toString(exception.getStackTrace()));

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, exception);

        // then
        Exception ex = assertThrows(RetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode409() {
        // given
        final int statusCodeNotFound = 409;
        String message = String.format(ERROR_MSG, statusCodeNotFound, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeNotFound, "", new HttpHeaders());

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder));

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testHandleApiErrorResponseExceptionWhenStatusCode400() {
        // given
        final int statusCodeNotFound = 400;
        String message = String.format(ERROR_MSG, statusCodeNotFound, APPOINTMENTS_DELTA_TYPE, COMPANY_NUMBER);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCodeNotFound, "", new HttpHeaders());

        // when
        Executable actual = () -> metricsApiResponseHandler.handle(COMPANY_NUMBER, APPOINTMENTS_DELTA_TYPE, new ApiErrorResponseException(builder));

        // then
        Exception ex = assertThrows(NonRetryableErrorException.class, actual);
        assertEquals(message, ex.getMessage());
    }
}

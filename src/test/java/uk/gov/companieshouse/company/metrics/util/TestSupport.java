package uk.gov.companieshouse.company.metrics.util;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;

//TODO delete this class as it's unused.
public class TestSupport {

    public static ApiErrorResponseException buildApiErrorResponseCustomException(int nonHttpStatusCode) {

        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                nonHttpStatusCode,
                "some error",
                new HttpHeaders()
        ).build();

        return ApiErrorResponseException.fromHttpResponseException(httpResponseException);
    }

    public static ApiErrorResponseException buildApiErrorResponseException(HttpStatus httpStatus) {

        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                new HttpHeaders()
        ).build();

        return ApiErrorResponseException.fromHttpResponseException(httpResponseException);
    }
}

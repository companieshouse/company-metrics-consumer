package uk.gov.companieshouse.company.metrics.util;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.company.metrics.consumer.ChargesStreamConsumer;
import uk.gov.companieshouse.company.metrics.type.ResourceChange;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class TestSupport {

    public static final String COMPANY_NUMBER = "01203396";
    public static final String MOCK_CHARGE_ID = "MYdKM_YnzAmJ8JtSgVXr61n1bgg";
    public static final String VALID_COMPANY_LINKS_PATH = String.format("/company/%s/charges/%s", COMPANY_NUMBER, MOCK_CHARGE_ID);
    public static final String INVALID_COMPANY_LINKS_PATH = String.format("/companyabc/%s/charges/%s", COMPANY_NUMBER, MOCK_CHARGE_ID);
    public static final String EVENT_TYPE_CHANGED = "changed";
    public static final String RESOURCE_KIND = "company-charges";
    public static final String CONTEXT_ID = "context_id";

    public Message<ResourceChangedData> createResourceChangedMessage(String companyLinksPath,
                                                                     boolean isDelete) throws IOException {
        return this.createResourceChangedMessage(companyLinksPath, COMPANY_NUMBER, MOCK_CHARGE_ID, isDelete);
    }

    public Message<ResourceChangedData> createResourceChangedMessage(String companyLinksPath,
                                                                     String companyNumber,
                                                                     String chargeId,
                                                                     boolean isDelete) throws IOException {
        InputStreamReader exampleChargesJsonPayload = new InputStreamReader(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader()
                        .getResourceAsStream("charges-record.json")));
        String chargesRecord = FileCopyUtils.copyToString(exampleChargesJsonPayload);

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(isDelete ? ChargesStreamConsumer.DELETE_EVENT_TYPE : EVENT_TYPE_CHANGED);

        ResourceChangedData resourceChangedData = ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(companyNumber)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyLinksPath, companyNumber, chargeId))
                .setEvent(eventRecord)
                .setData(chargesRecord)
                .build();

        return MessageBuilder
                .withPayload(resourceChangedData)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "test")
                .build();
    }

    public MetricsRecalculateApi createMetricsRecalculateApiData() {

        MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
        InternalData internalData = new InternalData();
        internalData.setUpdatedBy("topic-partition-offset");
        metricsRecalculateApi.setMortgage(Boolean.TRUE);
        metricsRecalculateApi.setAppointments(Boolean.FALSE);
        metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
        metricsRecalculateApi.setInternalData(internalData);
        return metricsRecalculateApi;
    }

    @NotNull
    public ResponseStatusException getResponseStatusException(int statusCode) {
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                statusCode, httpStatus.getReasonPhrase(), new
                HttpHeaders()).build();

        final ResponseStatusException responseStatusException =
                new ResponseStatusException(httpStatus, httpStatus.getReasonPhrase(),
                        ApiErrorResponseException.fromHttpResponseException(httpResponseException));
        return responseStatusException;
    }

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

    public ResourceChange createResourceChange() {
        ResourceChangedData resourceChangedData = new ResourceChangedData();
        return new ResourceChange(resourceChangedData);
    }
}

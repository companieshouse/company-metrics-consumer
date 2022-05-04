package uk.gov.companieshouse.company.metrics.model;

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
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class TestData {

    private static final String COMPANY_NUMBER = "02588581";
    public static final String VALID_COMPANY_LINKS_PATH = "/company/%s/charges";
    public static final String INVALID_COMPANY_LINKS_PATH = "/companyabc/%s/charges";

    public Message<ResourceChangedData> createResourceChangedMessage(String companyLinksPath) throws IOException {
        InputStreamReader exampleChargesJsonPayload = new InputStreamReader(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader()
                        .getResourceAsStream("charges-record.json")));
        String chargesRecord = FileCopyUtils.copyToString(exampleChargesJsonPayload);

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType("charges");

        ResourceChangedData resourceChangedData = ResourceChangedData.newBuilder()
                .setContextId("context_id")
                .setResourceId(COMPANY_NUMBER)
                .setResourceKind("company-charges")
                .setResourceUri(String.format(companyLinksPath, COMPANY_NUMBER))
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

}

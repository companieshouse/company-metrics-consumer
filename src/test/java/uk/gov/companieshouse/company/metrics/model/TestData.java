package uk.gov.companieshouse.company.metrics.model;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class TestData {

    private static final String COMPANY_NUMBER = "02588581";

    public Message<ResourceChangedData> createResourceChangedMessage() throws IOException {
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
                .setResourceUri(String.format("/company/%s/charges", COMPANY_NUMBER))
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
}

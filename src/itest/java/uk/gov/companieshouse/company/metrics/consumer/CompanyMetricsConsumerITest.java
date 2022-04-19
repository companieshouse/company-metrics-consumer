package uk.gov.companieshouse.company.metrics.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.company.metrics.AbstractIntegrationTest;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.util.Arrays;

public class CompanyMetricsConsumerITest extends AbstractIntegrationTest {

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${charges.stream.topic}")
    private String mainTopic;

    @Test
    public void testSendingKafkaMessage() {
        EventRecord event = EventRecord.newBuilder()
                .setType("changed")
                .setPublishedAt("2022-03-18T10:51:30")
                .setFieldsChanged(Arrays.asList("abc", "xyz"))
                .build();

        ResourceChangedData resourceChanged = ResourceChangedData.newBuilder()
                .setContextId("context_id")
                .setResourceId("12345678")
                .setResourceKind("company-charges")
                .setResourceUri("/company/12345678/charges")
                .setData("{ \"key\": \"value\" }")
                .setEvent(event)
                .build();

        kafkaTemplate.send(mainTopic, resourceChanged);
    }

}

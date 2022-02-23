package uk.gov.companieshouse.company.metrics.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.company.metrics.AbstractIntegrationTest;
import uk.gov.companieshouse.delta.ChsDelta;

public class CompanyMetricsConsumerITest extends AbstractIntegrationTest {

    @Autowired
    public KafkaTemplate<String, ChsDelta> kafkaTemplate;

    @Value("${company.metrics.topic.main}")
    private String mainTopic;

    @Test
    public void testSendingKafkaMessage() {
        ChsDelta chsDelta = new ChsDelta("{ \"key\": \"value\" }", 1, "some_id");
        kafkaTemplate.send(mainTopic, chsDelta);
    }

}

package uk.gov.companieshouse.company.metrics.consumer.officers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_COMPANY_OFFICERS_TOPIC;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.kafka.AbstractKafkaTest;
import uk.gov.companieshouse.company.metrics.kafka.TestConsumerAspect;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.util.TestUtils;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@WireMockTest(httpPort = 8888)
class OfficersStreamConsumerRetryableExceptionTest extends AbstractKafkaTest {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;
    @Autowired
    private KafkaProducer<String, byte[]> testProducer;
    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @MockBean
    private MetricsRouter router;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("steps", () -> 4);
    }

    @BeforeEach
    public void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void testRepublishToErrorTopicThroughRetryTopics() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        doThrow(RetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC, 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 6);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_TOPIC)).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC)).isEqualTo(3);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC)).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC)).isZero();
    }
}

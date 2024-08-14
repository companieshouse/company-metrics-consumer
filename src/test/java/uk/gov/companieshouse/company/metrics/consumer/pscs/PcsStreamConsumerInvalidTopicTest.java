package uk.gov.companieshouse.company.metrics.consumer.pscs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC;
import static uk.gov.companieshouse.company.metrics.util.TestUtils.STREAM_PSC_STATEMENTS_TOPIC;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.companieshouse.company.metrics.kafka.AbstractKafkaTest;
import uk.gov.companieshouse.company.metrics.kafka.TestConsumerAspect;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.util.TestUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SpringBootTest
@WireMockTest(httpPort = 8888)
class PcsStreamConsumerInvalidTopicTest extends AbstractKafkaTest {

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
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void testPublishPscStatementToInvalidMessageTopicIfInvalidDataDeserialised() throws InterruptedException, IOException, ExecutionException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("hello", encoder);

        //when
        Future<RecordMetadata> future = testProducer.send(new ProducerRecord<>(STREAM_PSC_STATEMENTS_TOPIC, 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_PSC_STATEMENTS_TOPIC), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC), is(1));
    }
}

package uk.gov.companieshouse.company.metrics.consumer;

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
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.processor.MetricsRouter;
import uk.gov.companieshouse.company.metrics.util.TestConfig;
import uk.gov.companieshouse.company.metrics.util.TestUtils;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CompanyMetricsConsumerApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(
        topics = {"stream-company-charges", "stream-company-charges-company-metrics-consumer-retry", "stream-company-charges-company-metrics-consumer-error"
        , "stream-company-charges-company-metrics-consumer-invalid"},
        controlledShutdown = true,
        partitions = 1
)
@TestPropertySource(locations = "classpath:application-test_consumer_main.yml")
@Import(TestConfig.class)
@ActiveProfiles("test_consumer_main")
class ChargesStreamConsumerTest {

    private static final int MESSAGE_CONSUMED_TIMEOUT = 5;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Autowired
    private ResettableCountDownLatch resettableCountDownLatch;

    @MockBean
    private MetricsRouter router;

    @BeforeEach
    public void beforeEach() {
        resettableCountDownLatch.resetLatch(4);
    }

    @Test
    void testConsumeMessage() throws IOException, InterruptedException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        testProducer.send(new ProducerRecord<>("stream-company-charges", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, 10000L, 1);

        //then
        assertThat(records.count()).isEqualTo(1);
        verify(router).route(any(), any(), any());
    }

    @Test
    void testRepublishToInvalidMessageTopicIfNonRetryableExceptionThrown() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(NonRetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>("stream-company-charges", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges")).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-retry")).isZero();
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-error")).isZero();
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-invalid")).isEqualTo(1);
    }

    @Test
    void testRepublishToErrorTopicThroughRetryTopics() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(RetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>("stream-company-charges", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 6);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges")).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-retry")).isEqualTo(3);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-error")).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-invalid")).isZero();
    }

    @Test
    void testPublishToInvalidMessageTopicIfInvalidDataDeserialised() throws InterruptedException, IOException, ExecutionException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("hello", encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        Future<RecordMetadata> future = testProducer.send(new ProducerRecord<>("stream-company-charges", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges")).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-retry")).isZero();
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-error")).isZero();
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-charges-company-metrics-consumer-invalid")).isEqualTo(1);
    }
}

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
import org.assertj.core.api.Assertions;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CompanyMetricsConsumerApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(
        topics = {"stream-psc-statements", "stream-psc-statements-company-metrics-consumer-retry"
                , "stream-psc-statements-company-metrics-consumer-error", "stream-psc-statements-company-metrics-consumer-invalid",
                "stream-company-psc", "stream-company-psc-company-metrics-consumer-retry"
                , "stream-company-psc-company-metrics-consumer-error", "stream-company-psc-company-metrics-consumer-invalid"},
        controlledShutdown = true,
        partitions = 1
)
@TestPropertySource(locations = "classpath:application-test_consumer_main.yml")
@Import(TestConfig.class)
@ActiveProfiles("test_consumer_main")
class PscEventStreamConsumerTest {

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
    void testConsumePscStatementMessage() throws IOException, InterruptedException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        testProducer.send(new ProducerRecord<>("stream-psc-statements", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        Assertions.assertThat(resettableCountDownLatch.getCountDownLatch().await(MESSAGE_CONSUMED_TIMEOUT, TimeUnit.SECONDS)).isTrue();

        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, 10000L, 1);

        //then
        assertThat(records.count(), is(1));
        verify(router).route(any(), any(), any());
    }
    @Test
    void testConsumePscMessage() throws IOException, InterruptedException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        testProducer.send(new ProducerRecord<>("stream-company-psc", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        Assertions.assertThat(resettableCountDownLatch.getCountDownLatch().await(MESSAGE_CONSUMED_TIMEOUT, TimeUnit.SECONDS)).isTrue();

        ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, 10000L, 1);

        //then
        assertThat(records.count(), is(1));
        verify(router).route(any(), any(), any());
    }

    @Test
    void testRepublishPscStatementToInvalidMessageTopicIfNonRetryableExceptionThrown() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(NonRetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>("stream-psc-statements", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-retry"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-error"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-invalid"), is(1));
    }

    @Test
    void testRepublishPscToInvalidMessageTopicIfNonRetryableExceptionThrown() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(NonRetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>("stream-company-psc", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-retry"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-error"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-invalid"), is(1));
    }

    @Test
    void testRepublishPscStatementToErrorTopicThroughRetryTopics() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(RetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>("stream-psc-statements", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 6);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-retry"), is(3));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-error"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-invalid"), is(0));
    }

    @Test
    void testRepublishPscToErrorTopicThroughRetryTopics() throws InterruptedException, IOException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("resource_kind", "resource_uri", "context_id", "resource_id", "{}",
                new EventRecord("published_at", "event_type", null)), encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);
        doThrow(RetryableErrorException.class).when(router).route(any(), any(), any());

        //when
        testProducer.send(new ProducerRecord<>("stream-company-psc", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        if (!resettableCountDownLatch.getCountDownLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 6);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-retry"), is(3));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-error"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-invalid"), is(0));
    }

    @Test
    void testPublishPscStatementToInvalidMessageTopicIfInvalidDataDeserialised() throws InterruptedException, IOException, ExecutionException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("hello", encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        Future<RecordMetadata> future = testProducer.send(new ProducerRecord<>("stream-psc-statements", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-retry"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-error"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-psc-statements-company-metrics-consumer-invalid"), is(1));
    }

    @Test
    void testPublishPscToInvalidMessageTopicIfInvalidDataDeserialised() throws InterruptedException, IOException, ExecutionException {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("hello", encoder);
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        Future<RecordMetadata> future = testProducer.send(new ProducerRecord<>("stream-company-psc", 0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc"), is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-retry"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-error"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-psc-company-metrics-consumer-invalid"), is(1));
    }
}
package uk.gov.companieshouse.company.metrics.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public final class TestUtils {

    public static final String STREAM_COMPANY_CHARGES_TOPIC = "stream-company-charges";
    public static final String STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_RETRY_TOPIC = "stream-company-charges-company-metrics-consumer-retry";
    public static final String STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_ERROR_TOPIC = "stream-company-charges-company-metrics-consumer-error";
    public static final String STREAM_COMPANY_CHARGES_COMPANY_METRICS_CONSUMER_INVALID_TOPIC = "stream-company-charges-company-metrics-consumer-invalid";
    public static final String STREAM_COMPANY_OFFICERS_TOPIC = "stream-company-officers";
    public static final String STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC = "stream-company-officers-company-metrics-consumer-retry";
    public static final String STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC = "stream-company-officers-company-metrics-consumer-error";
    public static final String STREAM_COMPANY_OFFICERS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC = "stream-company-officers-company-metrics-consumer-invalid";
    public static final String STREAM_PSC_STATEMENTS_TOPIC = "stream-psc-statements";
    public static final String STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_RETRY_TOPIC = "stream-psc-statements-company-metrics-consumer-retry";
    public static final String STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_ERROR_TOPIC = "stream-psc-statements-company-metrics-consumer-error";
    public static final String STREAM_PSC_STATEMENTS_COMPANY_METRICS_CONSUMER_INVALID_TOPIC = "stream-psc-statements-company-metrics-consumer-invalid";


    private TestUtils(){
    }

    public static int noOfRecordsForTopic(ConsumerRecords<?, ?> records, String topic) {
        int count = 0;
        for (ConsumerRecord<?, ?> ignored : records.records(topic)) {
            count++;
        }
        return count;
    }
}

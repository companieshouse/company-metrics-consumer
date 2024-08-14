package uk.gov.companieshouse.company.metrics.consumer;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class KafkaMessageConsumerAspect {

    private final int steps;
    private CountDownLatch latch;

    public KafkaMessageConsumerAspect(@Value("${steps:1}") int steps) {
        this.steps = steps;
        this.latch = new CountDownLatch(steps);
    }

    @After(
            "execution(* uk.gov.companieshouse.company.metrics.consumer.ChargesStreamConsumer" +
                    ".receive(..))" +
                    "|| execution(* uk.gov.companieshouse.company.metrics.consumer.OfficersStreamConsumer" +
                    ".receive(..))" +
                    "|| execution(* uk.gov.companieshouse.company.metrics.consumer.PscEventStreamConsumer" +
                    ".receive(..))")
    void onSuccessfulProcessing(JoinPoint joinPoint) {
        latch.countDown();
    }

    @AfterThrowing(value = "execution(* uk.gov.companieshouse.company.metrics.consumer.ChargesStreamConsumer" +
            ".receive(..))", throwing = "ex")
    void onConsumerException(Exception ex) {
        if (ex instanceof NonRetryableErrorException) {
            latch.countDown();
        } else {
            latch.countDown();
        }
    }

    @AfterThrowing("execution(* org.apache.kafka.common.serialization.Deserializer.deserialize(..))")
    void deserialize() {
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void resetLatch() {
        latch = new CountDownLatch(steps);
    }

}

package uk.gov.companieshouse.company.metrics.consumer;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;

@Aspect
public class KafkaMessageConsumerAspect {

    @Autowired
    private ResettableCountDownLatch resettableCountDownLatch;

    @AfterReturning("execution(* uk.gov.companieshouse.company.metrics.consumer.CompanyMetricsConsumer" +
            ".receive(..))")
    void onSuccessfulProcessing() {
        resettableCountDownLatch.countDownAll();
    }

    @AfterThrowing(value = "execution(* uk.gov.companieshouse.company.metrics.consumer.CompanyMetricsConsumer" +
            ".receive(..))", throwing = "ex")
    void onConsumerException(Exception ex) {
        if (ex instanceof NonRetryableErrorException) {
            resettableCountDownLatch.countDownAll();
        } else {
            resettableCountDownLatch.countDown();
        }
    }

    @AfterThrowing("execution(* org.apache.kafka.common.serialization.Deserializer.deserialize(..))")
    void deserialize() {
        resettableCountDownLatch.countDownAll();
    }
}

package uk.gov.companieshouse.company.metrics.util;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.consumer.ResettableCountDownLatch;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;

@Aspect
@Component
public class ConsumerAspect {

    @Autowired
    private ResettableCountDownLatch resettableCountDownLatch;

    @AfterReturning(
            "execution(* uk.gov.companieshouse.company.metrics.consumer.ChargesStreamConsumer" +
                ".receive(..))" +
            "|| execution(* uk.gov.companieshouse.company.metrics.consumer.OfficersStreamConsumer" +
                ".receive(..))" +
            "|| execution(* uk.gov.companieshouse.company.metrics.consumer.PscsStreamConsumer" +
                ".receive(..))")
    void onSuccessfulProcessing() {
        resettableCountDownLatch.countDownAll();
    }

    @AfterThrowing(value =
            "execution(* uk.gov.companieshouse.company.metrics.consumer.ChargesStreamConsumer" +
                ".receive(..))" +
            "|| execution(* uk.gov.companieshouse.company.metrics.consumer.OfficersStreamConsumer" +
                ".receive(..))" +
            "|| execution(* uk.gov.companieshouse.company.metrics.consumer.PscsStreamConsumer" +
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

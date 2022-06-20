package uk.gov.companieshouse.company.metrics.consumer;

import java.util.concurrent.CountDownLatch;

public class ResettableCountDownLatch {

    private CountDownLatch countDownLatch;

    public void resetLatch(int count) {
        countDownLatch = new CountDownLatch(count);
    }

    public void countDown() {
        countDownLatch.countDown();
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void countDownAll() {
        while (countDownLatch.getCount() > 0) {
            countDownLatch.countDown();
        }
    }
}

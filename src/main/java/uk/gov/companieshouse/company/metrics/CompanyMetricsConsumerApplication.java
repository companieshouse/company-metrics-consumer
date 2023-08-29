package uk.gov.companieshouse.company.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompanyMetricsConsumerApplication {

    public static final String NAMESPACE = "company-metrics-consumer";

    public static void main(String[] args) {
        SpringApplication.run(CompanyMetricsConsumerApplication.class, args);
    }
}

package uk.gov.companieshouse.company.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static final String APPLICATION_NAME_SPACE = "company-metrics-consumer";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

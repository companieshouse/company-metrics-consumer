package uk.gov.companieshouse.company.metrics.kafka;

import org.springframework.context.annotation.Import;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.company.metrics.util.TestConfig;

@Testcontainers
@Import(TestConfig.class)
public abstract class AbstractKafkaTest {

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(
            "confluentinc/cp-kafka:latest"));
}

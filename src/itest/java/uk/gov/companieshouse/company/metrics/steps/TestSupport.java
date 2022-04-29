package uk.gov.companieshouse.company.metrics.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static java.lang.Thread.sleep;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.PartitionInfo;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.ResourceUtils;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class TestSupport {
    private final Logger logger;
    KafkaTemplate<String, Object> kafkaTemplate;

    private static WireMockServer wireMockServer = null;

    public TestSupport(Logger logger, KafkaTemplate<String, Object> kafkaTemplate) {
      this.logger = logger;
      this.kafkaTemplate = kafkaTemplate;
    }

    public String loadFile(String dir, String fileName) {
        final String filePath = "classpath:" + dir + "/" + fileName;
        try {
            return FileUtils.readFileToString(ResourceUtils.getFile(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to locate file %s at %s", fileName, filePath));
        }
    }

    public String loadAvroMessageFile(String fileName) {
        return loadFile("Avro_Messages", fileName);
    }

    public List<ServeEvent> getServeEvents(){
        return wireMockServer != null ? wireMockServer.getAllServeEvents() :
              new ArrayList<ServeEvent>();
    }

  public WireMockServer setupWiremock() {
    if (wireMockServer == null){
      wireMockServer = new WireMockServer(8888);
      wireMockServer.start();
      configureFor("localhost", wireMockServer.port());
    }
    else{
      resetWiremock();
    }
    return wireMockServer;
  }

  public void resetWiremock() {
    if (wireMockServer == null){
      throw new RuntimeException("Wiremock not initialised");
    }
    wireMockServer.resetRequests();
  }
}

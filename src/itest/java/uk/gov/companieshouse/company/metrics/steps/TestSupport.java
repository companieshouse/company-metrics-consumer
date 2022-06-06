package uk.gov.companieshouse.company.metrics.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.ResourceUtils;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class TestSupport {

    KafkaTemplate<String, Object> kafkaTemplate;

    private static WireMockServer wireMockServer = null;
    public static final String RESOURCE_KIND = "company-charges";
    public static final String CONTEXT_ID = "context_id";
    public static final String VALID_COMPANY_CHARGES_LINK = "/company/%s/charges";
    public static final String RESOURCE_ID = "11223344";
    public static final String TYPE = "charges";
    public static final String DELETE_TYPE = "deleted";

    public TestSupport(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public ResourceChangedData createResourceChangedMessage(String companyChargesLink,
                                                            String companyNumber)
            throws IOException {

        String chargesRecord = loadFile("payloads", "charges-record.json");

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(TYPE);

        ResourceChangedData resourceChangedData = ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyChargesLink, companyNumber))
                .setEvent(eventRecord)
                .setData(chargesRecord)
                .build();

        return resourceChangedData;
    }

    public ResourceChangedData createResourceDeletedMessage(String companyChargesLink, String companyNumber, String payload) {

        String chargesRecord = loadFile("payloads", payload);

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(DELETE_TYPE);
        eventRecord.setFieldsChanged(null);

        ResourceChangedData resourceChangedData = ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyChargesLink, companyNumber))
                .setEvent(eventRecord)
                .setData(chargesRecord)
                .build();

        return resourceChangedData;
    }

    public List<ServeEvent> getServeEvents() {
        return wireMockServer != null ? wireMockServer.getAllServeEvents() :
            new ArrayList<>();
    }

    public WireMockServer setupWiremock() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(8888);
            wireMockServer.start();
            configureFor("localhost", wireMockServer.port());
        } else {
            resetWiremock();
        }
        return wireMockServer;
    }

    public void resetWiremock() {
        if (wireMockServer == null) {
            throw new RuntimeException("Wiremock not initialised");
        }
        wireMockServer.resetRequests();
        wireMockServer.resetAll();
        wireMockServer.stop();
        wireMockServer.start();
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
        return loadFile("payloads", fileName);
    }
}

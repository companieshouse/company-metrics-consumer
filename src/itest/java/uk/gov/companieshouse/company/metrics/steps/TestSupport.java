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
    public static final String RESOURCE_KIND = "resource_kind";
    public static final String CONTEXT_ID = "context_id";
    public static final String RESOURCE_ID = "11223344";
    public static final String CHANGED_EVENT_TYPE = "changed";
    public static final String DELETED_EVENT_TYPE = "deleted";

    public TestSupport(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public ResourceChangedData createResourceChangedMessageCharges(String companyChargesLink,
                                                                   String companyNumber,
                                                                   String chargeId) {

        String chargesRecord = loadFile("charges-record.json");

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(CHANGED_EVENT_TYPE);

        return ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyChargesLink, companyNumber, chargeId))
                .setEvent(eventRecord)
                .setData(chargesRecord)
                .build();
    }

    public ResourceChangedData createResourceDeletedMessageCharges(String companyChargesLink, String companyNumber, String chargedId, String payload) {

        String chargesRecord = loadFile(payload);

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(DELETED_EVENT_TYPE);
        eventRecord.setFieldsChanged(null);

        return ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyChargesLink, companyNumber, chargedId))
                .setEvent(eventRecord)
                .setData(chargesRecord)
                .build();
    }

    public ResourceChangedData createResourceChangedMessageAppointments(String companyAppointmentsLink,
                                                                        String companyNumber, String eventType)
    {
        String appointmentRecord = loadFile("appointment-record.json");

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(eventType);

        return ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyAppointmentsLink, companyNumber))
                .setEvent(eventRecord)
                .setData(appointmentRecord)
                .build();
    }

    public ResourceChangedData createResourceChangedMessagePsc(String pscLink,
                                                                        String companyNumber, String eventType)
    {
        String pscRecord = loadFile("psc-record.json");

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(eventType);

        return ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(pscLink, companyNumber))
                .setEvent(eventRecord)
                .setData(pscRecord)
                .build();
    }

    public ResourceChangedData createResourceChangedMessageInvalidAppointments(String companyAppointmentsLink,
                                                                               String companyNumber)
    {
        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType(CHANGED_EVENT_TYPE);

        return ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(String.format(companyAppointmentsLink, companyNumber))
                .setEvent(eventRecord)
                .setData("invalidAppointmentsData")
                .build();
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

    public String loadFile(String fileName) {
        final String filePath = "src/itest/resources/payloads/" + fileName;
        try {
            return FileUtils.readFileToString(ResourceUtils.getFile(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to locate file %s at %s", fileName, filePath));
        }
    }

    public String loadAvroMessageFile(String fileName) {
        return loadFile(fileName);
    }
}

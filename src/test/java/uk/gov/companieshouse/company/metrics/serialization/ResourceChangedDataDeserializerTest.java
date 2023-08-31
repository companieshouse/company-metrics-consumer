package uk.gov.companieshouse.company.metrics.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

class ResourceChangedDataDeserializerTest {

    private ResourceChangedDataDeserializer deserializer;

    @BeforeEach
    public void init() {
        deserializer = new ResourceChangedDataDeserializer();
    }

    @Test
    void When_deserialize_Expect_ValidResourceChangedDataObject() {
        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType("charges");
        ResourceChangedData resourceChangedData = new ResourceChangedData("resource_kind","resource_uri","context_id","resource_id","data",eventRecord);
        byte[] data = encodedData(resourceChangedData);

        ResourceChangedData deserializedObject = deserializer.deserialize("", data);

        assertThat(deserializedObject).isEqualTo(resourceChangedData);
    }

    private byte[] encodedData(ResourceChangedData resourceChangedData){
        ResourceChangedDataSerializer serializer = new ResourceChangedDataSerializer();
        return serializer.serialize("", resourceChangedData);
    }

    @Test
    void When_deserializeFails_throwsNonRetryableError() {
        byte[] data = "Invalid message".getBytes();
        assertThrows(NonRetryableErrorException.class, () -> deserializer.deserialize("", data));
    }
}

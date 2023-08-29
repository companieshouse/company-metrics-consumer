package uk.gov.companieshouse.company.metrics.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedDataSerializerTest {

    private ResourceChangedDataSerializer serializer;

    @BeforeEach
    public void init() {
        serializer = new ResourceChangedDataSerializer();
    }

    @Test
    void When_serialize_Expect_resourceChangedDataBytes() {

        EventRecord eventRecord = new EventRecord();
        eventRecord.setPublishedAt("2022010351");
        eventRecord.setType("charges");
        ResourceChangedData resourceChangedData = new ResourceChangedData("resource_kind","resource_uri","context_id","resource_id","data",eventRecord);
        byte[] result = serializer.serialize("", resourceChangedData);

        assertThat(decodedData(result)).isEqualTo(resourceChangedData);
    }

    @Test
    void When_serialize_null_returns_null() {
        byte[] serialize = serializer.serialize("", null);
        assertThat(serialize).isNull();
    }

    @Test
    void When_serialize_receivesBytes_returnsBytes() {
        byte[] byteExample = "Sample bytes".getBytes();
        byte[] serialize = serializer.serialize("", byteExample);
        assertThat(serialize).isEqualTo(byteExample);
    }

    @Test
    void When_serializeFails_throwsNonRetryableError() {
        Object payload = mock(Object.class);
        when(payload.toString()).thenThrow(new RuntimeException());
        assertThrows(NonRetryableErrorException.class, () -> serializer.serialize("", payload));
    }

    private ResourceChangedData decodedData(byte[] resourceChangedData) {
        ResourceChangedDataDeserializer serializer = new ResourceChangedDataDeserializer();
        return serializer.deserialize("", resourceChangedData);
    }
}

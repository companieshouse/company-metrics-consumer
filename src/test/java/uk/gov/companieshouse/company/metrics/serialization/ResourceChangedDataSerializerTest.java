package uk.gov.companieshouse.company.metrics.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import javax.management.ObjectName;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResourceChangedDataSerializerTest {

    @Mock
    private Logger logger;

    private ResourceChangedDataSerializer serializer;

    @BeforeEach
    public void init() {
        serializer = new ResourceChangedDataSerializer(logger);
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
        assertThat(serialize).isEqualTo(null);
    }

    @Test
    void When_serialize_receivesBytes_returnsBytes() {
        byte[] byteExample = "Sample bytes".getBytes();
        byte[] serialize = serializer.serialize("", byteExample);
        assertThat(serialize).isEqualTo(byteExample);
    }

    @Test
    void When_serializeFails_throwsNonRetryableError() {
        int[] newIntArray = new int[1];
        when(serializer.serialize("", newIntArray)).thenThrow(NonRetryableErrorException.class);
        assertThrows(NonRetryableErrorException.class, () -> serializer.serialize("", newIntArray));
    }

    private ResourceChangedData decodedData(byte[] resourceChangedData) {
        ResourceChangedDataDeserializer serializer = new ResourceChangedDataDeserializer(this.logger);
        return serializer.deserialize("", resourceChangedData);
    }
}

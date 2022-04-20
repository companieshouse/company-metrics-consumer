package uk.gov.companieshouse.company.metrics.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.company.metrics.service.api.serialization.ResourceChangedDataDeserializer;
import uk.gov.companieshouse.company.metrics.service.api.serialization.ResourceChangedDataSerializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ResourceChangedDataDeserializerTest {

    @Mock
    private Logger logger;
    private ResourceChangedDataDeserializer deserializer;

    @BeforeEach
    public void init() {
        deserializer = new ResourceChangedDataDeserializer(logger);
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
        ResourceChangedDataSerializer serializer = new ResourceChangedDataSerializer(this.logger);
        return serializer.serialize("", resourceChangedData);
    }
}

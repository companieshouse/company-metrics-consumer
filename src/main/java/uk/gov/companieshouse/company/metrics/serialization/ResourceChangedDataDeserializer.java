package uk.gov.companieshouse.company.metrics.serialization;

import java.util.Arrays;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ResourceChangedDataDeserializer implements Deserializer<ResourceChangedData> {

    private final Logger logger;

    @Autowired
    public ResourceChangedDataDeserializer(Logger logger) {
        this.logger = logger;
    }


    /**
     * deserialize.
     */
    @Override
    public ResourceChangedData deserialize(String topic, byte[] data) {
        try {
            logger.trace(String.format("DSND-599: Message picked up from topic with data: %s",
                    new String(data)));
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<ResourceChangedData> reader =
                    new ReflectDatumReader<>(ResourceChangedData.class);
            ResourceChangedData resourceChangedData = reader.read(null, decoder);
            logger.trace(String.format("DSND-599: Message successfully de-serialised into "
                    + "Avro ResourceChangedData object: %s", resourceChangedData));
            return resourceChangedData;
        } catch (Exception ex) {
            logger.error("Serialization exception while converting to Avro schema object", ex);
            throw new SerializationException(
                    "Message data [" + Arrays.toString(data)
                            + "] from topic ["
                            + topic
                            + "] cannot be deserialized", ex);
        }
    }

}

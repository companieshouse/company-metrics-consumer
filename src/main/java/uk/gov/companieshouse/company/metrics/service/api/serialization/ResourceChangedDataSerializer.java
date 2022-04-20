package uk.gov.companieshouse.company.metrics.service.api.serialization;

import java.nio.charset.StandardCharsets;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableException;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ResourceChangedDataSerializer implements Serializer<Object> {

    private final Logger logger;

    @Autowired
    public ResourceChangedDataSerializer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public byte[] serialize(String topic, Object payload) {
        logger.trace("Payload serialised: " + payload);

        try {
            if (payload == null) {
                return null;
            }

            if (payload instanceof byte[]) {
                return (byte[]) payload;
            }

            if (payload instanceof ResourceChangedData) {
                ResourceChangedData resourceChangedData = (ResourceChangedData) payload;
                DatumWriter<ResourceChangedData> writer = new SpecificDatumWriter<>();
                EncoderFactory encoderFactory = EncoderFactory.get();

                AvroSerializer<ResourceChangedData> avroSerializer =
                        new AvroSerializer<>(writer, encoderFactory);

                return avroSerializer.toBinary(resourceChangedData);
            }

            return payload.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.error("Serialization exception while writing to byte array", ex);
            throw new NonRetryableException("Serialization exception while "
                    + "writing to byte array", ex);
        }
    }
}

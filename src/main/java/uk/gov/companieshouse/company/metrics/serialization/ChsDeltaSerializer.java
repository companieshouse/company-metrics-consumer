package uk.gov.companieshouse.company.metrics.serialization;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;

@Component
public class ChsDeltaSerializer implements Serializer<ChsDelta> {

    @Override
    public byte[] serialize(String var1, ChsDelta chsDelta) {
        try {
            DatumWriter<ChsDelta> writer = new SpecificDatumWriter<>();
            EncoderFactory encoderFactory = EncoderFactory.get();

            AvroSerializer<ChsDelta> avroSerializer = new AvroSerializer<>(writer, encoderFactory);

            return avroSerializer.toBinary(chsDelta);
        } catch (SerializationException ex) {
            throw new RuntimeException(ex);
        }
    }
}

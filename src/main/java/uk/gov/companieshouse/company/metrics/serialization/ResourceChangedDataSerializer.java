package uk.gov.companieshouse.company.metrics.serialization;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.APPLICATION_NAME_SPACE;

import java.nio.charset.StandardCharsets;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ResourceChangedDataSerializer implements Serializer<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public byte[] serialize(String topic, Object payload) {

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
            LOGGER.error("Serialization exception while writing to byte array",
                    ex, DataMapHolder.getLogMap());
            throw new NonRetryableErrorException("Serialization exception while "
                    + "writing to byte array", ex);
        }
    }
}

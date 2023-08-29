package uk.gov.companieshouse.company.metrics.serialization;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.NAMESPACE;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ResourceChangedDataDeserializer implements Deserializer<ResourceChangedData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    /**
     * deserialize.
     */
    @Override
    public ResourceChangedData deserialize(String topic, byte[] data) {
        try {
            LOGGER.trace(String.format("Message picked up from topic with data: %s",
                    new String(data)), DataMapHolder.getLogMap());
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<ResourceChangedData> reader =
                    new ReflectDatumReader<>(ResourceChangedData.class);
            ResourceChangedData resourceChangedData = reader.read(null, decoder);
            LOGGER.trace(String.format("Message successfully de-serialised into "
                    + "Avro ResourceChangedData object: %s", resourceChangedData),
                    DataMapHolder.getLogMap());
            return resourceChangedData;
        } catch (Exception ex) {
            LOGGER.error("De-Serialization exception while converting to Avro schema object",
                    ex, DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(ex);
        }
    }

}

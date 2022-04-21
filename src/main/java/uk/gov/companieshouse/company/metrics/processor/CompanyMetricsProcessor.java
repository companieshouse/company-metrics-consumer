package uk.gov.companieshouse.company.metrics.processor;

import static uk.gov.companieshouse.company.metrics.processor.ResponseHandler.handleResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.RetryableException;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.service.api.serialization.producer.CompanyMetricsProducer;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class CompanyMetricsProcessor {

    private final CompanyMetricsProducer metricsProducer;
    private final CompanyMetricsApiTransformer transformer;
    private final Logger logger;
    private final CompanyMetricsApiService companyMetricsApiService;

    /**
     * Construct a Company Metrics processor.
     */
    @Autowired
    public CompanyMetricsProcessor(CompanyMetricsProducer metricsProducer,
                                   CompanyMetricsApiTransformer transformer,
                                   Logger logger,
                                   CompanyMetricsApiService companyMetricsApiService
    ) {
        this.metricsProducer = metricsProducer;
        this.transformer = transformer;
        this.logger = logger;
        this.companyMetricsApiService = companyMetricsApiService;
    }

    /**
     * Process ResourceChangedData message.
     */
    public void process(Message<ResourceChangedData> resourceChangedMessage,
                        String topic,
                        String partition,
                        String offset) {
        try {
            logger.trace(String.format("DSND-599: ResourceChangedData extracted "
                    + "from a Kafka message: %s", resourceChangedMessage));

            MessageHeaders headers = resourceChangedMessage.getHeaders();
            final ResourceChangedData payload = resourceChangedMessage.getPayload();
            final String contextId = payload.getContextId();
            final Map<String, Object> logMap = new HashMap<>();
            final String companyNumber = extractCompanyNumber(payload.getResourceUri());
            final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);


            logger.trace(String.format("Company number %s extracted from"
                            + " ResourceURI %s the payload is %s ", companyNumber,
                    payload.getResourceUri(), payload));

            MetricsRecalculateApi metricsRecalculateApi = new MetricsRecalculateApi();
            InternalData internalData = new InternalData();
            internalData.setUpdatedBy(updatedBy);
            metricsRecalculateApi.setMortgage(Boolean.TRUE);
            metricsRecalculateApi.setAppointments(Boolean.FALSE);
            metricsRecalculateApi.setPersonsWithSignificantControl(Boolean.FALSE);
            metricsRecalculateApi.setInternalData(internalData);



            final ApiResponse<Void> postResponse =
                    companyMetricsApiService.postCompanyMetrics(
                            contextId, companyNumber,
                            metricsRecalculateApi
                    );

            logger.trace(String.format("Performing a POST with company profile %s",
                    metricsRecalculateApi));
            handleResponse(HttpStatus.valueOf(postResponse.getStatusCode()), contextId,
                    "Response from PATCH call to company profile api", logMap, logger);




        } catch (RetryableException ex) {
            retryMetricsMessage(resourceChangedMessage);
        } catch (Exception ex) {
            handleErrorMessage(resourceChangedMessage);
            // send to error topic
        }
    }

    public void retryMetricsMessage(Message<ResourceChangedData> resourceChangedMessage) {
    }

    private void handleErrorMessage(Message<ResourceChangedData> resourceChangedMessage) {
    }

    private String extractCompanyNumber(String resourceUri) {

        if (StringUtils.isNotBlank(resourceUri)) {
            //matches all characters between company/ and /
            Pattern companyNo = Pattern.compile("(?<=company/)(.*?)(?=/)");
            Matcher matcher = companyNo.matcher(resourceUri);
            if (matcher.find()) {
                return matcher.group(0).length() > 1 ? matcher.group(0) : null;
            }
        }
        return null;
    }


}

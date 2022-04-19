package uk.gov.companieshouse.company.metrics.processor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableException;
import uk.gov.companieshouse.company.metrics.exception.RetryableException;
import uk.gov.companieshouse.company.metrics.producer.CompanyMetricsProducer;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Component
public class CompanyMetricsProcessor {

    private final CompanyMetricsProducer metricsProducer;
    private final CompanyMetricsApiTransformer transformer;
    private final Logger logger;

    /**
     * Construct a Company Metrics processor.
     */
    @Autowired
    public CompanyMetricsProcessor(CompanyMetricsProducer metricsProducer,
                                   CompanyMetricsApiTransformer transformer,
                                   Logger logger) {
        this.metricsProducer = metricsProducer;
        this.transformer = transformer;
        this.logger = logger;
    }

    /**
     * Process ResourceChangedData message.
     */
    public void process(Message<ResourceChangedData> resourceChangedMessage) {
        try {
            logger.trace(String.format("DSND-599: ResourceChangedData extracted "
                    + "from a Kafka message: %s", resourceChangedMessage));

            MessageHeaders headers = resourceChangedMessage.getHeaders();
            final ResourceChangedData payload = resourceChangedMessage.getPayload();

            final String companyNumber = extractCompanyNumber(payload.getResourceUri());

            logger.trace(String.format("Company number %s extracted from"
                            + " ResourceURI %s the payload is %s ", companyNumber,
                    payload.getResourceUri(), payload));

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

    private void handleResponse(
            final ResponseStatusException ex,
            final HttpStatus httpStatus,
            final String logContext,
            final String msg,
            final Map<String, Object> logMap)
            throws NonRetryableException, RetryableException {
        logMap.put("status", httpStatus.toString());
        if (HttpStatus.BAD_REQUEST == httpStatus) {
            // 400 BAD REQUEST status is not retry-able
            throw new NonRetryableException(String
                    .format("Bad request POST Api Response %s", msg));
        } else if (!httpStatus.is2xxSuccessful()) {
            // any other client or server status is retry-able
            logger.errorContext(logContext, msg + ", retry", null, logMap);
            throw new RetryableException(String
                    .format("Unsuccessful POST API response, %s", msg));
        } else {
            logger.trace("Got success response from POST recalculate metrics");
        }
    }


}

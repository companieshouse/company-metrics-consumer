package uk.gov.companieshouse.company.metrics.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.metrics.InternalData;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Component
public class CompanyMetricsProcessor {

    private final CompanyMetricsApiTransformer transformer;
    private final Logger logger;
    private final CompanyMetricsApiService companyMetricsApiService;

    /**
     * Construct a Company Metrics processor.
     */
    @Autowired
    public CompanyMetricsProcessor(CompanyMetricsApiTransformer transformer,
                                   Logger logger,
                                   CompanyMetricsApiService companyMetricsApiService) {
        this.transformer = transformer;
        this.logger = logger;
        this.companyMetricsApiService = companyMetricsApiService;
    }

    /**
     * Process ResourceChangedData message.
     */
    public void process(ResourceChangedData payload,
                        String topic,
                        String partition,
                        String offset) {
        logger.trace(String.format("DSND-599: ResourceChangedData extracted "
                + "from a Kafka message: %s", payload));

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

        logger.trace(String.format("Performing company metrics recalculate operation %s",
                metricsRecalculateApi));

        handleResponse(HttpStatus.valueOf(postResponse.getStatusCode()), contextId, logMap);

    }

    /**
     * extract company number from
     * Resource URI.
     */
    public String extractCompanyNumber(String resourceUri) {

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
            final HttpStatus httpStatus,
            final String logContext,
            final Map<String, Object> logMap)
            throws NonRetryableErrorException, RetryableErrorException {
        logMap.put("status", httpStatus.toString());
        if (HttpStatus.BAD_REQUEST == httpStatus) {
            // 400 BAD REQUEST status is not retry-able
            String message = "400 BAD_REQUEST response received from company-metrics-api";
            logger.errorContext(logContext, message, null, logMap);
            throw new NonRetryableErrorException(message);
        } else if (!httpStatus.is2xxSuccessful()) {
            // any other client or server status is retry-able
            String message = "Non-Successful response received from company-metrics-api";
            logger.errorContext(logContext, message + ", retry", null, logMap);
            throw new RetryableErrorException(message);
        } else {
            String message = "Got success response from POST endpoint of company-metrics-api";
            logger.debugContext(logContext, message, logMap);
        }
    }

}

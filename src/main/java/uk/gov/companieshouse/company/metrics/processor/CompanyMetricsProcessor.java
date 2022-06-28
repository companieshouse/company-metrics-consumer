package uk.gov.companieshouse.company.metrics.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsRecalculateApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.exception.RetryableErrorException;
import uk.gov.companieshouse.company.metrics.service.ChargesDataApiService;
import uk.gov.companieshouse.company.metrics.service.CompanyMetricsApiService;
import uk.gov.companieshouse.company.metrics.transformer.CompanyMetricsApiTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class CompanyMetricsProcessor {

    public static final int HTTP_STATUS_GONE = 410;
    public static final String COMPANY_NUMBER_URI_PATTERN = "(?<=company/)(.*?)(?=/)";
    private final CompanyMetricsApiTransformer metricsApiTransformer;
    private final Logger logger;
    private final CompanyMetricsApiService companyMetricsApiService;
    private final ChargesDataApiService chargesDataApiService;

    /**
     * Construct a Company Metrics processor.
     */
    @Autowired
    public CompanyMetricsProcessor(CompanyMetricsApiTransformer metricsApiTransformer,
                                   Logger logger,
                                   CompanyMetricsApiService companyMetricsApiService,
                                   ChargesDataApiService chargesDataApiService) {
        this.metricsApiTransformer = metricsApiTransformer;
        this.logger = logger;
        this.companyMetricsApiService = companyMetricsApiService;
        this.chargesDataApiService = chargesDataApiService;
    }

    /**
     * Process ResourceChangedData message.
     */
    public void process(ResourceChangedData payload, String topic,
                        String partition, String offset) {
        final String contextId = payload.getContextId();
        String resourceUri = payload.getResourceUri();
        final Optional<String> companyNumberOptional = extractCompanyNumber(resourceUri);
        final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);
        MetricsProcessor<String, String, String, String> updateProcessor =
                (contId, uri, compNumber, updated) ->
                        processForUpdate(contId, uri, compNumber, updated);
        processMetrics(contextId, resourceUri, companyNumberOptional, updatedBy, updateProcessor);
    }

    /**
     * Process delete message.
     */
    public void processDelete(ResourceChangedData payload, String topic,
                        String partition, String offset) {
        final String contextId = payload.getContextId();
        String resourceUri = payload.getResourceUri();
        final Optional<String> companyNumberOptional = extractCompanyNumber(resourceUri);
        final String updatedBy = String.format("%s-%s-%s", topic, partition, offset);
        MetricsProcessor<String, String, String, String> deleteProcessor =
                (contId, uri, compNumber, updated) ->
                        processForDelete(contId, uri, compNumber, updated);
        processMetrics(contextId, resourceUri, companyNumberOptional, updatedBy, deleteProcessor);
    }

    private void processMetrics(String contextId, String resourceUri,
                                Optional<String> companyNumberOptional,
                                String updatedBy,
                                MetricsProcessor<String, String, String, String> processor) {
        companyNumberOptional
                .filter(Predicate.not(String::isBlank))
                .ifPresentOrElse(companyNumber -> {
                    logger.info(String.format("Company number %s extracted from"
                                    + " resourceURI %s", companyNumber, resourceUri));
                    processor.apply(contextId, resourceUri, updatedBy, companyNumber);
                },
                        () -> {
                            throw new NonRetryableErrorException("Unable to extract "
                                    + "company number due to invalid resource uri in the message");
                        });
    }

    private void processForUpdate(String contextId, String resourceUri, String updatedBy,
                                  String companyNumber) {
        if (isChargeAvailable(resourceUri, contextId)) {
            prepareAndInvokeMetricsApi(companyNumber, contextId, updatedBy);
        } else {
            throw new RetryableErrorException("Charge details could not be found!");
        }
    }

    private void processForDelete(String contextId, String resourceUri, String updatedBy,
                                  String companyNumber) {
        if (isChargeAlreadyDeleted(resourceUri, contextId)) {
            prepareAndInvokeMetricsApi(companyNumber, contextId, updatedBy);
        } else {
            throw new RetryableErrorException("Charge details found!");
        }
    }

    private boolean isChargeAvailable(String resourceUri, String contextId) {
        ApiResponse<ChargeApi> apiResponseFromChargesDataApi =
                prepareAndInvokeChargesDataApi(resourceUri, contextId);
        if (apiResponseFromChargesDataApi == null) {
            return false;
        }
        HttpStatus statusCode = HttpStatus.valueOf(apiResponseFromChargesDataApi.getStatusCode());
        if (statusCode.is2xxSuccessful()) {
            return true;
        }
        return false;
    }

    private boolean isChargeAlreadyDeleted(String resourceUri, String contextId) {
        ApiResponse<ChargeApi> apiResponseFromChargesDataApi =
                prepareAndInvokeChargesDataApi(resourceUri, contextId);
        return apiResponseFromChargesDataApi.getStatusCode() == HTTP_STATUS_GONE ? true : false;
    }


    private void prepareAndInvokeMetricsApi(String companyNumber, String contextId,
                                            String updatedBy) {
        MetricsRecalculateApi metricsRecalculateApi = metricsApiTransformer.transform(updatedBy);
        logger.trace(String.format("Performing company metrics recalculate operation %s",
                metricsRecalculateApi));
        Map<String, Object> logMap = createLogMap(companyNumber, "POST");

        try {
            ApiResponse<Void> apiResponse = companyMetricsApiService.invokeMetricsPostApi(
                    contextId, companyNumber, metricsRecalculateApi);
            handleResponse(HttpStatus.valueOf(apiResponse.getStatusCode()), contextId, logMap);
        } catch (ResponseStatusException exception) {
            handleResponse(exception.getStatus(), contextId, logMap);
        }
    }

    /**
     * extract company number from Resource URI.
     */
    public Optional<String> extractCompanyNumber(String resourceUri) {

        if (StringUtils.isNotBlank(resourceUri)) {
            //matches all characters between company/ and /
            Pattern companyNo = Pattern.compile(COMPANY_NUMBER_URI_PATTERN);
            Matcher matcher = companyNo.matcher(resourceUri);
            if (matcher.find()) {
                return Optional.ofNullable(matcher.group());
            }
        }
        return Optional.empty();
    }

    private void handleResponse(final HttpStatus httpStatus, final String logContext,
            final Map<String, Object> logMap) {
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
            logger.info(String.format("Successfully invoked company-metrics-api "
                            + "POST endpoint for message with contextId: %s",
                    logContext));
        }
    }

    private Map<String, Object> createLogMap(String logInfo, String method) {
        final Map<String, Object> logMap = new HashMap<>();
        logMap.put("company_number", logInfo);
        logMap.put("method", method);
        return logMap;
    }

    private ApiResponse<ChargeApi> prepareAndInvokeChargesDataApi(String uri, String contextId) {
        logger.trace(String.format("Performing get operation on charges data api with uri %s",
                uri));
        Map<String, Object> logMap = createLogMap(uri, "GET");
        return chargesDataApiService.getACharge(contextId, uri);
    }

}

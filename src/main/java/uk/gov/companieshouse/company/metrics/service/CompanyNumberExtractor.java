package uk.gov.companieshouse.company.metrics.service;

import static uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication.APPLICATION_NAME_SPACE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.company.metrics.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class CompanyNumberExtractor implements CompanyNumberExtractable {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private static final Pattern EXTRACT_COMPANY_NUMBER_PATTERN =
            Pattern.compile("(?<=company/)([a-zA-Z0-9]{6,10})(?=/.*)");
    private static final String NULL_EMPTY_URI =
            "Could not extract company number from empty or null resource uri";
    private static final String EXTRACTION_ERROR =
            "Could not extract company number from resource URI: %s";

    @Override
    public String extractCompanyNumber(String uri) {
        if (StringUtils.isBlank(uri)) {
            LOGGER.error(NULL_EMPTY_URI, DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(NULL_EMPTY_URI);
        }
        // matches up to 10 digits between company/ and /
        Matcher matcher = EXTRACT_COMPANY_NUMBER_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group();
        } else {
            LOGGER.error(String.format(EXTRACTION_ERROR, uri), DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(String.format(EXTRACTION_ERROR, uri));
        }
    }
}

package uk.gov.companieshouse.company.metrics.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class CompanyNumberExtractor implements CompanyNumberExtractable {
    private static final Pattern EXTRACT_COMPANY_NUMBER_PATTERN =
           Pattern.compile("(?<=company/)([a-zA-Z0-9]{6,10})(?=/.*)");

    private static final String NULL_EMPTY_URI = "Could not extract company number from empty or null resource uri";

    private static final String EXTRACTION_ERROR = "Could not extract company number from resource URI: ";

    private final Logger logger;

    public CompanyNumberExtractor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String extractCompanyNumber(String uri) {
        if (StringUtils.isBlank(uri)) {
            logger.error(NULL_EMPTY_URI);
            throw new NonRetryableErrorException(NULL_EMPTY_URI);
        }
        // matches up to 10 digits between company/ and /
        Matcher matcher = EXTRACT_COMPANY_NUMBER_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group();
        } else {
            logger.error(String.format(EXTRACTION_ERROR
                    + "%s", uri));
            throw new NonRetryableErrorException(
                    String.format(EXTRACTION_ERROR + "%s", uri));
        }
    }
}

package uk.gov.companieshouse.company.metrics.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.company.metrics.exception.NonRetryableErrorException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class CompanyNumberExtractor implements CompanyNumberExtractable {
    private static final String EXTRACT_COMPANY_NUMBER_PATTERN =
            "(?<=company/)([a-zA-Z0-9]{6,10})(?=/.*)";

    private final Logger logger;

    public CompanyNumberExtractor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String extractCompanyNumber(String uri) {
        if (StringUtils.isBlank(uri)) {
            logger.error("Could not extract company number from empty or null resource uri");
            throw new NonRetryableErrorException(
                    "Could not extract company number from empty or null resource uri");
        }
        // matches up to 10 digits between company/ and /
        Pattern companyNo = Pattern.compile(EXTRACT_COMPANY_NUMBER_PATTERN);
        Matcher matcher = companyNo.matcher(uri);
        if (matcher.find()) {
            return matcher.group();
        } else {
            logger.error(String.format("Could not extract company number from uri "
                    + "%s ", uri));
            throw new NonRetryableErrorException(
                    String.format("Could not extract company number from resource URI: %s", uri));
        }
    }
}

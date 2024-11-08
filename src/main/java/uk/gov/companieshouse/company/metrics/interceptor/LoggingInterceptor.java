package uk.gov.companieshouse.company.metrics.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.company.metrics.CompanyMetricsConsumerApplication;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.logging.util.RequestLogger;

@Component
public class LoggingInterceptor implements AsyncHandlerInterceptor, RequestLogger {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CompanyMetricsConsumerApplication.APPLICATION_NAME_SPACE);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        logStartRequestProcessing(request, LOGGER);
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           @Nullable ModelAndView modelAndView) {

        logEndRequestProcessing(request, response, LOGGER);
    }
}
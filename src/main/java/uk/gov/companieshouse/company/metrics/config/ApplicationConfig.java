package uk.gov.companieshouse.company.metrics.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    @Bean
    SerializerFactory serializerFactory() {
        return new SerializerFactory();
    }

    @Bean
    EnvironmentReader environmentReader() {
        return new EnvironmentReaderImpl();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public InternalApiClient internalApiClient() {
        return ApiSdkManager.getPrivateSDK();
    }
}

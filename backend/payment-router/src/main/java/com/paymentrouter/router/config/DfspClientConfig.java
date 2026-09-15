package com.paymentrouter.router.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP client used by the DFSP adapters.
 * <p>
 * Timeouts make sure a slow or unreachable DFSP cannot block a request thread forever.
 * They are configured with {@code dfsp.client.connect-timeout} and {@code dfsp.client.read-timeout}.
 */
@Configuration
public class DfspClientConfig {

    @Bean
    public RestClient dfspRestClient(
            @Value("${dfsp.client.connect-timeout}") Duration connectTimeout,
            @Value("${dfsp.client.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}

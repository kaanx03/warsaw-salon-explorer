package com.kaandev.salonexplorer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({GooglePlacesProperties.class, IngestionProperties.class})
public class RestClientConfig {

    @Bean
    public RestClient googlePlacesRestClient(GooglePlacesProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.readTimeoutMs()));

        return RestClient.builder()
            .baseUrl(props.baseUrl())
            .requestFactory(factory)
            .defaultHeader("X-Goog-Api-Key", props.apiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}

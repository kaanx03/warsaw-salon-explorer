package com.kaandev.salonexplorer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.places")
public record GooglePlacesProperties(
    String apiKey,
    String baseUrl,
    String languageCode,
    String regionCode,
    int connectTimeoutMs,
    int readTimeoutMs
) {
}

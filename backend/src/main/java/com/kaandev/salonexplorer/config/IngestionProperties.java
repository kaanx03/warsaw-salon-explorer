package com.kaandev.salonexplorer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
    int batchSize,
    int rateLimitPerSecond,
    int maxRetries,
    long retryBackoffMs,
    List<String> searchQueries
) {
}

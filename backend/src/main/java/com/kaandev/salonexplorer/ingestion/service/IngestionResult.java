package com.kaandev.salonexplorer.ingestion.service;

public record IngestionResult(
    int totalFetched,
    int inserted,
    int updated,
    int skipped,
    int failed,
    long durationMs
) {
    public static IngestionResult empty() {
        return new IngestionResult(0, 0, 0, 0, 0, 0);
    }

    public IngestionResult merge(IngestionResult other) {
        return new IngestionResult(
            this.totalFetched + other.totalFetched,
            this.inserted + other.inserted,
            this.updated + other.updated,
            this.skipped + other.skipped,
            this.failed + other.failed,
            this.durationMs + other.durationMs
        );
    }
}

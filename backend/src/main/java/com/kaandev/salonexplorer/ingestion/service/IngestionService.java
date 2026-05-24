package com.kaandev.salonexplorer.ingestion.service;

import com.kaandev.salonexplorer.config.IngestionProperties;
import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.ingestion.client.GooglePlacesClient;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.ingestion.normalizer.SalonNormalizer;
import com.kaandev.salonexplorer.repository.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final GooglePlacesClient placesClient;
    private final SalonRepository salonRepository;
    private final SalonNormalizer normalizer;
    private final IngestionProperties props;

    public IngestionResult ingestAll() {
        log.info("==> Starting ingestion. Queries: {}", props.searchQueries());
        var start = Instant.now();
        var result = IngestionResult.empty();

        for (String query : props.searchQueries()) {
            try {
                var queryResult = ingestQuery(query);
                result = result.merge(queryResult);
                log.info("Query '{}' done: {}", query, queryResult);
            } catch (Exception e) {
                log.error("Query '{}' failed: {}", query, e.getMessage(), e);
            }
        }

        var duration = Duration.between(start, Instant.now()).toMillis();
        log.info("==> Ingestion complete in {}ms: {}", duration, result);
        return result;
    }

    private IngestionResult ingestQuery(String query) {
        var start = Instant.now();
        int inserted = 0, updated = 0, skipped = 0, failed = 0;

        List<PlaceDto> places = placesClient.searchAllPages(query);
        int fetched = places.size();

        for (PlaceDto place : places) {
            try {
                var outcome = upsertSalon(place);
                switch (outcome) {
                    case INSERTED -> inserted++;
                    case UPDATED  -> updated++;
                    case SKIPPED  -> skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Failed to ingest placeId={}: {}", place.id(), e.getMessage());
            }
        }

        var duration = Duration.between(start, Instant.now()).toMillis();
        return new IngestionResult(fetched, inserted, updated, skipped, failed, duration);
    }

    @Transactional
    protected UpsertOutcome upsertSalon(PlaceDto place) {
        if (place.id() == null || place.displayName() == null || place.formattedAddress() == null) {
            log.debug("Skipping place with missing required fields: {}", place.id());
            return UpsertOutcome.SKIPPED;
        }

        Optional<Salon> existing = salonRepository.findByGooglePlaceId(place.id());

        if (existing.isPresent()) {
            Salon salon = existing.get();
            normalizer.applyUpdates(salon, place);
            salonRepository.save(salon);
            return UpsertOutcome.UPDATED;
        } else {
            Salon salon = normalizer.toEntity(place);
            salonRepository.save(salon);
            return UpsertOutcome.INSERTED;
        }
    }

    enum UpsertOutcome { INSERTED, UPDATED, SKIPPED }
}

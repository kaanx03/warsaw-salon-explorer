package com.kaandev.salonexplorer.ingestion.service;

import com.kaandev.salonexplorer.ingestion.client.GooglePlacesClient;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.ingestion.normalizer.SalonNormalizer;
import com.kaandev.salonexplorer.repository.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentService {

    private final SalonRepository salonRepository;
    private final GooglePlacesClient googlePlacesClient;
    private final SalonNormalizer salonNormalizer;

    @Transactional
    public EnrichmentResult enrichAll() {
        var salons = salonRepository.findAllByIsActiveTrue();
        int enriched = 0;
        int skipped = 0;
        int failed = 0;

        for (var salon : salons) {
            if (salon.getGooglePlaceId() == null) {
                skipped++;
                continue;
            }
            try {
                PlaceDto detail = googlePlacesClient.fetchDetail(salon.getGooglePlaceId());
                if (detail != null) {
                    salonNormalizer.applyEnrichment(salon, detail);
                    salonRepository.save(salon);
                    enriched++;
                } else {
                    skipped++;
                }
            } catch (Exception ex) {
                log.warn("Enrichment failed for salon id={} placeId={}: {}", salon.getId(), salon.getGooglePlaceId(), ex.getMessage());
                failed++;
            }
        }

        log.info("Enrichment complete: enriched={}, skipped={}, failed={}", enriched, skipped, failed);
        return new EnrichmentResult(enriched, skipped, failed);
    }

    public record EnrichmentResult(int enriched, int skipped, int failed) {}
}

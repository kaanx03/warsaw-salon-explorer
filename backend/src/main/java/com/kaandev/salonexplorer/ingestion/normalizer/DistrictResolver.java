package com.kaandev.salonexplorer.ingestion.normalizer;

import com.kaandev.salonexplorer.domain.entity.District;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistrictResolver {

    private static final List<String> DISTRICT_TYPE_PRIORITIES = List.of(
        "sublocality_level_1",
        "administrative_area_level_3",
        "locality"
    );

    private final DistrictRepository districtRepository;
    private final Map<String, Optional<District>> cache = new ConcurrentHashMap<>();

    public District resolve(PlaceDto place) {
        if (place.addressComponents() == null) return null;

        for (String typePriority : DISTRICT_TYPE_PRIORITIES) {
            for (var component : place.addressComponents()) {
                if (component.types() == null) continue;
                if (component.types().contains(typePriority)) {
                    District d = lookup(component.longText());
                    if (d != null) return d;
                    d = lookup(component.shortText());
                    if (d != null) return d;
                }
            }
        }
        log.debug("No district resolved for place: {}", place.id());
        return null;
    }

    private District lookup(String rawName) {
        if (rawName == null || rawName.isBlank()) return null;
        String slug = toSlug(rawName);
        return cache.computeIfAbsent(slug, this::queryDb).orElse(null);
    }

    private Optional<District> queryDb(String slug) {
        return districtRepository.findBySlug(slug);
    }

    private String toSlug(String name) {
        String lower = name.toLowerCase().trim();
        lower = lower
            .replace("ł", "l").replace("ą", "a").replace("ę", "e")
            .replace("ó", "o").replace("ś", "s").replace("ć", "c")
            .replace("ź", "z").replace("ż", "z").replace("ń", "n");
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}

package com.kaandev.salonexplorer.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoProxyService {

    private final RestClient googlePlacesRestClient;

    private final Map<String, String> uriCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> refsCache = new ConcurrentHashMap<>();

    /**
     * Resolves a Google Places photo reference to a signed CDN URL.
     * Uses string concat (not URI templates) to avoid encoding slashes inside the ref path.
     */
    public String resolveToSignedUri(String photoRef) {
        return uriCache.computeIfAbsent(photoRef, ref -> {
            try {
                // NOTE: intentional string concat — Spring URI templates encode slashes in
                // the ref which breaks the Google Places API path structure.
                String path = "/" + ref + "/media?maxWidthPx=800&skipHttpRedirect=true";
                JsonNode node = googlePlacesRestClient.get()
                    .uri(path)
                    .retrieve()
                    .body(JsonNode.class);
                if (node != null && node.has("photoUri")) {
                    return node.get("photoUri").asText();
                }
            } catch (Exception e) {
                log.warn("Photo resolve failed ref={}: {}", ref, e.getMessage());
            }
            return null;
        });
    }

    public List<String> fetchPhotoRefs(String googlePlaceId) {
        return refsCache.computeIfAbsent(googlePlaceId, this::doFetchPhotoRefs);
    }

    private List<String> doFetchPhotoRefs(String googlePlaceId) {
        try {
            String path = "/places/" + googlePlaceId + "?fields=photos";
            JsonNode response = googlePlacesRestClient.get()
                .uri(path)
                .retrieve()
                .body(JsonNode.class);
            if (response == null || !response.has("photos")) return List.of();
            List<String> urls = new ArrayList<>();
            for (JsonNode photo : response.get("photos")) {
                String name = photo.get("name").asText();
                urls.add("/api/v1/photos?ref=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
            }
            return urls;
        } catch (Exception e) {
            log.warn("Photo refs fetch failed placeId={}: {}", googlePlaceId, e.getMessage());
            return List.of();
        }
    }
}

package com.kaandev.salonexplorer.ingestion.client;

import com.kaandev.salonexplorer.config.GooglePlacesProperties;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.ingestion.client.dto.PlacesSearchRequest;
import com.kaandev.salonexplorer.ingestion.client.dto.PlacesSearchResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private static final String SEARCH_FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location," +
        "places.rating,places.userRatingCount,places.priceLevel," +
        "places.internationalPhoneNumber,places.websiteUri,places.photos," +
        "places.addressComponents,nextPageToken";

    private final RestClient googlePlacesRestClient;
    private final GooglePlacesProperties props;

    @Retry(name = "googlePlacesApi")
    public PlacesSearchResponse searchText(String query) {
        log.info("Places Text Search: query='{}', lang={}", query, props.languageCode());
        var request = PlacesSearchRequest.of(query, props.languageCode(), props.regionCode());

        return googlePlacesRestClient.post()
            .uri("/places:searchText")
            .header("X-Goog-FieldMask", SEARCH_FIELD_MASK)
            .body(request)
            .retrieve()
            .body(PlacesSearchResponse.class);
    }

    public List<PlaceDto> searchAllPages(String query) {
        var response = searchText(query);
        return response != null ? response.placesOrEmpty() : List.of();
    }
}

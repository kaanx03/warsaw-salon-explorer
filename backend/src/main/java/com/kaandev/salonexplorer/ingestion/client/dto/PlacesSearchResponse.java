package com.kaandev.salonexplorer.ingestion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacesSearchResponse(
    List<PlaceDto> places,
    String nextPageToken
) {
    public List<PlaceDto> placesOrEmpty() {
        return places != null ? places : List.of();
    }
}

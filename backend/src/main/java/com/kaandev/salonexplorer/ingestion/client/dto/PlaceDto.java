package com.kaandev.salonexplorer.ingestion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceDto(
    String id,
    DisplayName displayName,
    String formattedAddress,
    Location location,
    Double rating,
    @JsonProperty("userRatingCount") Integer userRatingCount,
    @JsonProperty("priceLevel") String priceLevel,
    @JsonProperty("internationalPhoneNumber") String internationalPhoneNumber,
    @JsonProperty("websiteUri") String websiteUri,
    List<Photo> photos,
    List<AddressComponent> addressComponents,
    EditorialSummary editorialSummary,
    RegularOpeningHours regularOpeningHours
) {
    public record DisplayName(String text, String languageCode) {}
    public record Location(Double latitude, Double longitude) {}
    public record Photo(String name, Integer widthPx, Integer heightPx) {}
    public record AddressComponent(
        String longText,
        String shortText,
        List<String> types
    ) {}
    public record EditorialSummary(String text, String languageCode) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegularOpeningHours(List<String> weekdayDescriptions) {}
}

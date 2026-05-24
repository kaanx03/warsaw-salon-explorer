package com.kaandev.salonexplorer.ingestion.client.dto;

public record PlacesSearchRequest(
    String textQuery,
    String languageCode,
    String regionCode,
    Integer pageSize,
    String pageToken
) {
    public static PlacesSearchRequest of(String query, String lang, String region) {
        return new PlacesSearchRequest(query, lang, region, 20, null);
    }
}

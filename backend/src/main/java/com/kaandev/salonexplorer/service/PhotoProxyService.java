package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.config.GooglePlacesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PhotoProxyService {

    private final RestClient googlePlacesRestClient;
    private final GooglePlacesProperties props;

    @Cacheable(value = "photos", key = "#photoRef")
    public byte[] fetchPhoto(String photoRef) {
        return googlePlacesRestClient.get()
            .uri("/{ref}/media?maxWidthPx=800&key={key}", photoRef, props.apiKey())
            .retrieve()
            .body(byte[].class);
    }
}

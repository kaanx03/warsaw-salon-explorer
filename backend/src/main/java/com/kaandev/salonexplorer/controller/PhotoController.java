package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.service.PhotoProxyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Tag(name = "Photos", description = "Photo proxy — keeps Google API key server-side")
public class PhotoController {

    private final PhotoProxyService photoProxyService;

    @GetMapping
    public ResponseEntity<Void> getPhoto(@RequestParam("ref") String photoRef) {
        String signedUri = photoProxyService.resolveToSignedUri(photoRef);
        if (signedUri == null) return ResponseEntity.notFound().build();
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, signedUri)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .build();
    }
}

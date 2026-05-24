package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.service.PhotoProxyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Tag(name = "Photos", description = "Photo proxy — keeps Google API key server-side")
public class PhotoController {

    private final PhotoProxyService photoProxyService;

    @GetMapping("/{photoRef}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String photoRef) {
        String decoded = URLDecoder.decode(photoRef, StandardCharsets.UTF_8);
        byte[] image = photoProxyService.fetchPhoto(decoded);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header("Cache-Control", "public, max-age=86400")
            .body(image);
    }
}

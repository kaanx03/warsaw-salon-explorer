package com.kaandev.salonexplorer.domain.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {
    public static LoginResponse of(String token, long expiresIn) {
        return new LoginResponse(token, "Bearer", expiresIn);
    }
}

package com.example.hotelreservation.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

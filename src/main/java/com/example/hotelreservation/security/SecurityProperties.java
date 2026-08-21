package com.example.hotelreservation.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security")
@Validated
public record SecurityProperties(
        @NotBlank(message = "JWT secret must be provided")
        @Size(min = 32, message = "JWT secret must contain at least 32 characters")
        String jwtSecret,

        @Min(value = 60, message = "JWT expiration must be at least 60 seconds")
        long jwtExpirationSeconds
) {
}
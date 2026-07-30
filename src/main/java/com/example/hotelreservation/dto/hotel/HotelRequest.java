package com.example.hotelreservation.dto.hotel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HotelRequest(
        @NotBlank(message = "Hotel name is required")
        @Size(max = 150, message = "Hotel name must not exceed 150 characters")
        String name,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotNull(message = "Hotel stars are required")
        @Min(value = 1, message = "Hotel stars must be between 1 and 5")
        @Max(value = 5, message = "Hotel stars must be between 1 and 5")
        Integer stars
) {
}
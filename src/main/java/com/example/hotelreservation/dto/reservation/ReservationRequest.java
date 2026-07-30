package com.example.hotelreservation.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationRequest(
        @NotNull(message = "Hotel ID is required")
        Long hotelId,

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Check-in date is required")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOut,

        @NotNull(message = "Total price is required")
        @PositiveOrZero(message = "Total price cannot be negative")
        BigDecimal totalPrice
) {
}
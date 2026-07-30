package com.example.hotelreservation.dto.customer;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}

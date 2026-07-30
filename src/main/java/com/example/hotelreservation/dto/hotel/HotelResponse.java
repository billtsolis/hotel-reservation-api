package com.example.hotelreservation.dto.hotel;

public record HotelResponse(
        Long id,
        String name,
        String city,
        Integer stars
) {
}
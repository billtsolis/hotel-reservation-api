package com.example.hotelreservation.dto.reservation;

import com.example.hotelreservation.entity.ReservationStatus;

import java.time.LocalDate;

public record ReservationSearchCriteria(
        String hotelName,
        String customerName,
        String city,
        ReservationStatus status,
        LocalDate checkIn,
        LocalDate checkOut
) {
}
package com.example.hotelreservation.dto.reservation;

import com.example.hotelreservation.entity.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
        Long id,
        Long hotelId,
        String hotelName,
        Long customerId,
        String customerName,
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal totalPrice,
        ReservationStatus status
) {
}
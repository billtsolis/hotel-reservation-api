package com.example.hotelreservation.mapper;

import com.example.hotelreservation.dto.reservation.ReservationRequest;
import com.example.hotelreservation.dto.reservation.ReservationResponse;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Hotel;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public Reservation toEntity(
            ReservationRequest request,
            Hotel hotel,
            Customer customer
    ) {
        Reservation reservation = new Reservation();
        reservation.setHotel(hotel);
        reservation.setCustomer(customer);
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        reservation.setTotalPrice(request.totalPrice());
        reservation.setStatus(ReservationStatus.ACTIVE);

        return reservation;
    }

    public ReservationResponse toResponse(Reservation reservation) {
        Customer customer = reservation.getCustomer();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getHotel().getId(),
                reservation.getHotel().getName(),
                customer.getId(),
                customer.getFirstName() + " " + customer.getLastName(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getTotalPrice(),
                reservation.getStatus()
        );
    }
}
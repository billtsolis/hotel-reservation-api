package com.example.hotelreservation.mapper;


import com.example.hotelreservation.dto.hotel.HotelRequest;
import com.example.hotelreservation.dto.hotel.HotelResponse;
import com.example.hotelreservation.entity.Hotel;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public Hotel toEntity(HotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.name().trim());
        hotel.setCity(request.city().trim());
        hotel.setStars(request.stars());
        return hotel;
    }

    public HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                hotel.getStars()
        );
    }

    public void updateEntity(Hotel hotel, HotelRequest request) {
        hotel.setName(request.name().trim());
        hotel.setCity(request.city().trim());
        hotel.setStars(request.stars());
    }
}
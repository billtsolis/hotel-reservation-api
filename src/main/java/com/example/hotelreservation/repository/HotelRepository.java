package com.example.hotelreservation.repository;

import com.example.hotelreservation.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

}

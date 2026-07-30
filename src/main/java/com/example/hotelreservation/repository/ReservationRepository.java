package com.example.hotelreservation.repository;

import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.customer.id = :customerId
          AND r.status = :status
          AND r.checkIn < :newCheckOut
          AND r.checkOut > :newCheckIn
        """)
    boolean existsOverlappingReservation(
            @Param("customerId") Long customerId,
            @Param("status") ReservationStatus status,
            @Param("newCheckIn") LocalDate newCheckIn,
            @Param("newCheckOut") LocalDate newCheckOut
    );
    boolean existsByHotel_Id(Long hotelId);

}

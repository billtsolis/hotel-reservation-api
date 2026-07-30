package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.common.PageResponse;
import com.example.hotelreservation.dto.reservation.ReservationRequest;
import com.example.hotelreservation.dto.reservation.ReservationResponse;
import com.example.hotelreservation.dto.reservation.ReservationSearchCriteria;
import com.example.hotelreservation.entity.ReservationStatus;
import com.example.hotelreservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request
    ) {
        ReservationResponse createdReservation = reservationService.createReservation(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdReservation.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdReservation);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReservationResponse>>
    getAllReservations(
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "checkIn",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponse<ReservationResponse> response = reservationService.getAllReservations(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id
    ) {
        ReservationResponse response = reservationService.getReservationById(id);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id
    ) {
        reservationService.cancelReservation(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ReservationResponse>>
    searchReservations(

            @RequestParam(required = false)
            String hotelName,

            @RequestParam(required = false)
            String customerName,

            @RequestParam(required = false)
            String city,

            @RequestParam(required = false)
            ReservationStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut,

            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "checkIn",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        ReservationSearchCriteria criteria =
                new ReservationSearchCriteria(
                        hotelName,
                        customerName,
                        city,
                        status,
                        checkIn,
                        checkOut
                );

        PageResponse<ReservationResponse> response =
                reservationService.searchReservations(
                        criteria,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}
package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.common.PageResponse;
import com.example.hotelreservation.dto.hotel.HotelRequest;
import com.example.hotelreservation.dto.hotel.HotelResponse;
import com.example.hotelreservation.service.HotelService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(
            @Valid @RequestBody HotelRequest request
    ) {
        HotelResponse createdHotel = hotelService.createHotel(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdHotel.id())
                .toUri();

        return ResponseEntity.created(location).body(createdHotel);
    }

    @GetMapping
    public ResponseEntity<PageResponse<HotelResponse>> getAllHotels(
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        PageResponse<HotelResponse> response = hotelService.getAllHotels(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getHotelById(
            @PathVariable Long id
    ) {
        HotelResponse response = hotelService.getHotelById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelResponse> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody HotelRequest request
    ) {
        HotelResponse response = hotelService.updateHotel(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(
            @PathVariable Long id
    ) {
        hotelService.deleteHotel(id);

        return ResponseEntity.noContent().build();
    }
}
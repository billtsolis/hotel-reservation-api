package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.hotel.HotelRequest;
import com.example.hotelreservation.dto.hotel.HotelResponse;
import com.example.hotelreservation.entity.Hotel;
import com.example.hotelreservation.exception.ResourceInUseException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.HotelRepository;
import com.example.hotelreservation.exception.BusinessValidationException;
import com.example.hotelreservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelService hotelService;

    @Mock
    private ReservationRepository reservationRepository;

    @Test
    void createHotel_shouldSaveAndReturnResponse() {
        HotelRequest request = new HotelRequest(
                "Hilton Athens",
                "Athens",
                5
        );

        Hotel hotel = new Hotel();
        hotel.setName("Hilton Athens");
        hotel.setCity("Athens");
        hotel.setStars(5);

        Hotel savedHotel = new Hotel();
        savedHotel.setId(1L);
        savedHotel.setName("Hilton Athens");
        savedHotel.setCity("Athens");
        savedHotel.setStars(5);

        HotelResponse expectedResponse = new HotelResponse(
                1L,
                "Hilton Athens",
                "Athens",
                5
        );

        when(hotelMapper.toEntity(request)).thenReturn(hotel);
        when(hotelRepository.save(hotel)).thenReturn(savedHotel);
        when(hotelMapper.toResponse(savedHotel))
                .thenReturn(expectedResponse);

        HotelResponse result = hotelService.createHotel(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Hilton Athens", result.name());
        assertEquals("Athens", result.city());
        assertEquals(5, result.stars());

        verify(hotelMapper).toEntity(request);
        verify(hotelRepository).save(hotel);
        verify(hotelMapper).toResponse(savedHotel);
    }

    @Test
    void createHotel_shouldRejectStarsBelowOne() {
        HotelRequest request = new HotelRequest(
                "Invalid Hotel",
                "Athens",
                0
        );

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> hotelService.createHotel(request)
        );

        assertEquals(
                "Hotel stars must be between 1 and 5",
                exception.getMessage()
        );

        verifyNoInteractions(hotelRepository);
        verifyNoInteractions(hotelMapper);
    }

    @Test
    void createHotel_shouldRejectStarsAboveFive() {
        HotelRequest request = new HotelRequest(
                "Invalid Hotel",
                "Athens",
                6
        );

        assertThrows(
                BusinessValidationException.class,
                () -> hotelService.createHotel(request)
        );

        verifyNoInteractions(hotelRepository);
        verifyNoInteractions(hotelMapper);
    }

    @Test
    void getHotelById_shouldReturnHotelWhenFound() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Hilton Athens");
        hotel.setCity("Athens");
        hotel.setStars(5);

        HotelResponse response = new HotelResponse(
                1L,
                "Hilton Athens",
                "Athens",
                5
        );

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(hotel));

        when(hotelMapper.toResponse(hotel))
                .thenReturn(response);

        HotelResponse result = hotelService.getHotelById(1L);

        assertEquals(response, result);

        verify(hotelRepository).findById(1L);
        verify(hotelMapper).toResponse(hotel);
    }

    @Test
    void getHotelById_shouldThrowWhenHotelDoesNotExist() {
        when(hotelRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> hotelService.getHotelById(99L)
        );

        assertEquals(
                "Hotel not found with id: 99",
                exception.getMessage()
        );

        verify(hotelRepository).findById(99L);
        verifyNoInteractions(hotelMapper);
    }

    @Test
    void updateHotel_shouldUpdateExistingHotel() {
        HotelRequest request = new HotelRequest(
                "Updated Hotel",
                "Piraeus",
                4
        );

        Hotel existingHotel = new Hotel();
        existingHotel.setId(1L);
        existingHotel.setName("Old Hotel");
        existingHotel.setCity("Athens");
        existingHotel.setStars(3);

        HotelResponse expectedResponse = new HotelResponse(
                1L,
                "Updated Hotel",
                "Piraeus",
                4
        );

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(existingHotel));

        when(hotelRepository.save(existingHotel))
                .thenReturn(existingHotel);

        when(hotelMapper.toResponse(existingHotel))
                .thenReturn(expectedResponse);

        HotelResponse result =
                hotelService.updateHotel(1L, request);

        assertEquals(expectedResponse, result);

        verify(hotelMapper).updateEntity(existingHotel, request);
        verify(hotelRepository).save(existingHotel);
        verify(hotelMapper).toResponse(existingHotel);
    }

    @Test
    void deleteHotel_shouldRejectDeletionWhenReservationsExist() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(hotel));

        when(reservationRepository.existsByHotel_Id(1L))
                .thenReturn(true);

        ResourceInUseException exception = assertThrows(
                ResourceInUseException.class,
                () -> hotelService.deleteHotel(1L)
        );

        assertEquals("Hotel cannot be deleted because it has reservations", exception.getMessage());

        verify(hotelRepository).findById(1L);
        verify(reservationRepository).existsByHotel_Id(1L);
        verify(hotelRepository, never()).delete(any());
    }
}
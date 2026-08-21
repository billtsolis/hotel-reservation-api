package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.reservation.ReservationRequest;
import com.example.hotelreservation.dto.reservation.ReservationResponse;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Hotel;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.ReservationStatus;
import com.example.hotelreservation.exception.BusinessValidationException;
import com.example.hotelreservation.exception.ReservationConflictException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.ReservationMapper;
import com.example.hotelreservation.repository.CustomerRepository;
import com.example.hotelreservation.repository.HotelRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void createReservation_shouldSaveValidReservation() {
        ReservationRequest request = validRequest();

        Hotel hotel = createHotel();
        Customer customer = createCustomer();

        Reservation reservation = new Reservation();
        reservation.setHotel(hotel);
        reservation.setCustomer(customer);
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        reservation.setTotalPrice(request.totalPrice());
        reservation.setStatus(ReservationStatus.ACTIVE);

        Reservation savedReservation = new Reservation();
        savedReservation.setId(10L);
        savedReservation.setHotel(hotel);
        savedReservation.setCustomer(customer);
        savedReservation.setCheckIn(request.checkIn());
        savedReservation.setCheckOut(request.checkOut());
        savedReservation.setTotalPrice(request.totalPrice());
        savedReservation.setStatus(ReservationStatus.ACTIVE);

        ReservationResponse expectedResponse =
                createReservationResponse();

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(hotel));

        when(customerRepository.findById(2L))
                .thenReturn(Optional.of(customer));

        when(reservationRepository.existsOverlappingReservation(
                2L,
                ReservationStatus.ACTIVE,
                request.checkIn(),
                request.checkOut()
        )).thenReturn(false);

        when(reservationMapper.toEntity(
                request,
                hotel,
                customer
        )).thenReturn(reservation);

        when(reservationRepository.save(reservation))
                .thenReturn(savedReservation);

        when(reservationMapper.toResponse(savedReservation))
                .thenReturn(expectedResponse);

        ReservationResponse result =
                reservationService.createReservation(request);

        assertEquals(expectedResponse, result);
        assertEquals(ReservationStatus.ACTIVE, result.status());

        verify(hotelRepository).findById(1L);
        verify(customerRepository).findById(2L);

        verify(reservationRepository)
                .existsOverlappingReservation(
                        2L,
                        ReservationStatus.ACTIVE,
                        request.checkIn(),
                        request.checkOut()
                );

        verify(reservationRepository).save(reservation);
    }

    @Test
    void createReservation_shouldRejectCheckOutBeforeCheckIn() {
        ReservationRequest request =
                new ReservationRequest(
                        1L,
                        2L,
                        LocalDate.of(2026, 8, 15),
                        LocalDate.of(2026, 8, 10),
                        new BigDecimal("500.00")
                );

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals(
                "Check-out date must be after check-in date",
                exception.getMessage()
        );

        verifyNoInteractions(hotelRepository);
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(reservationRepository);
        verifyNoInteractions(reservationMapper);
    }

    @Test
    void createReservation_shouldRejectEqualCheckInAndCheckOut() {
        LocalDate date = LocalDate.of(2026, 8, 10);

        ReservationRequest request =
                new ReservationRequest(
                        1L,
                        2L,
                        date,
                        date,
                        new BigDecimal("500.00")
                );

        assertThrows(
                BusinessValidationException.class,
                () -> reservationService.createReservation(request)
        );

        verifyNoInteractions(hotelRepository);
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservation_shouldRejectNegativePrice() {
        ReservationRequest request =
                new ReservationRequest(
                        1L,
                        2L,
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 15),
                        new BigDecimal("-1.00")
                );

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals(
                "Total price cannot be negative",
                exception.getMessage()
        );

        verifyNoInteractions(hotelRepository);
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservation_shouldRejectOverlappingReservation() {
        ReservationRequest request = validRequest();

        Hotel hotel = createHotel();
        Customer customer = createCustomer();

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(hotel));

        when(customerRepository.findById(2L))
                .thenReturn(Optional.of(customer));

        when(reservationRepository.existsOverlappingReservation(
                2L,
                ReservationStatus.ACTIVE,
                request.checkIn(),
                request.checkOut()
        )).thenReturn(true);

        ReservationConflictException exception =
                assertThrows(
                        ReservationConflictException.class,
                        () -> reservationService.createReservation(request)
                );

        assertEquals(
                "Customer already has an active reservation "
                        + "that overlaps with the selected dates",
                exception.getMessage()
        );

        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(reservationMapper);
    }

    @Test
    void createReservation_shouldThrowWhenHotelDoesNotExist() {
        ReservationRequest request = validRequest();

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals(
                "Hotel not found with id: 1",
                exception.getMessage()
        );

        verify(hotelRepository).findById(1L);
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(reservationRepository);
        verifyNoInteractions(reservationMapper);
    }

    @Test
    void createReservation_shouldThrowWhenCustomerDoesNotExist() {
        ReservationRequest request = validRequest();
        Hotel hotel = createHotel();

        when(hotelRepository.findById(1L))
                .thenReturn(Optional.of(hotel));

        when(customerRepository.findById(2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals(
                "Customer not found with id: 2",
                exception.getMessage()
        );

        verify(hotelRepository).findById(1L);
        verify(customerRepository).findById(2L);
        verifyNoInteractions(reservationMapper);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void cancelReservation_shouldChangeStatusToCancelled() {
        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setStatus(ReservationStatus.ACTIVE);

        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(reservation))
                .thenReturn(reservation);

        reservationService.cancelReservation(10L);

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository).findById(10L);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelReservation_shouldNotSaveAlreadyCancelledReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(10L);
        reservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation));

        reservationService.cancelReservation(10L);

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository).findById(10L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_shouldThrowWhenReservationDoesNotExist() {
        when(reservationRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.cancelReservation(99L)
        );

        assertEquals(
                "Reservation not found with id: 99",
                exception.getMessage()
        );

        verify(reservationRepository).findById(99L);
        verify(reservationRepository, never()).save(any());
    }

    private ReservationRequest validRequest() {
        return new ReservationRequest(
                1L,
                2L,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 15),
                new BigDecimal("750.00")
        );
    }

    private Hotel createHotel() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Hilton Athens");
        hotel.setCity("Athens");
        hotel.setStars(5);
        return hotel;
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setId(2L);
        customer.setFirstName("John");
        customer.setLastName("Smith");
        customer.setEmail("john@example.com");
        return customer;
    }

    private ReservationResponse createReservationResponse() {
        return new ReservationResponse(
                10L,
                1L,
                "Hilton Athens",
                2L,
                "John Smith",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 15),
                new BigDecimal("750.00"),
                ReservationStatus.ACTIVE
        );
    }
}
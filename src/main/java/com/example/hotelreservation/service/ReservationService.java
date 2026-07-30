package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.common.PageResponse;
import com.example.hotelreservation.dto.reservation.ReservationRequest;
import com.example.hotelreservation.dto.reservation.ReservationResponse;
import com.example.hotelreservation.dto.reservation.ReservationSearchCriteria;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Hotel;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.ReservationStatus;
import com.example.hotelreservation.exception.BusinessValidationException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.ReservationMapper;
import com.example.hotelreservation.repository.CustomerRepository;
import com.example.hotelreservation.repository.HotelRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.specification.ReservationSpecifications;
import com.example.hotelreservation.util.PageableValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Service
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final HotelRepository hotelRepository;
    private final CustomerRepository customerRepository;
    private final ReservationMapper reservationMapper;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "checkIn", "checkOut", "totalPrice", "status");

    public ReservationService(
            ReservationRepository reservationRepository,
            HotelRepository hotelRepository,
            CustomerRepository customerRepository,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.hotelRepository = hotelRepository;
        this.customerRepository = customerRepository;
        this.reservationMapper = reservationMapper;
    }

    @Transactional
    public ReservationResponse createReservation(
            ReservationRequest request
    ) {
        log.info(
                "Creating reservation hotelId={} customerId={} checkIn={} checkOut={}",
                request.hotelId(),
                request.customerId(),
                request.checkIn(),
                request.checkOut()
        );

        validateReservationRequest(request);

        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hotel not found with id: "
                                        + request.hotelId()
                        )
                );

        Customer customer = customerRepository.findById(
                        request.customerId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id: "
                                        + request.customerId()
                        )
                );

        boolean overlapExists =
                reservationRepository.existsOverlappingReservation(
                        customer.getId(),
                        ReservationStatus.ACTIVE,
                        request.checkIn(),
                        request.checkOut()
                );

        if (overlapExists) {
            log.warn(
                    "Reservation rejected due to overlap customerId={} checkIn={} checkOut={}",
                    customer.getId(),
                    request.checkIn(),
                    request.checkOut()
            );

            throw new BusinessValidationException(
                    "Customer already has an active reservation "
                            + "that overlaps with the selected dates"
            );
        }

        Reservation reservation = reservationMapper.toEntity(request, hotel, customer);
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation created successfully id={}", savedReservation.getId());

        return reservationMapper.toResponse(savedReservation);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getAllReservations(Pageable pageable) {
        PageableValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        Page<Reservation> reservationPage = reservationRepository.findAll(pageable);

        return PageResponse.from(reservationPage, reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = findReservationById(id);

        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        log.info("Cancelling reservation id={}", id);

        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.info("Reservation is already cancelled id={}", id);
            return;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("Reservation cancelled successfully id={}", id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> searchReservations(
            ReservationSearchCriteria criteria,
            Pageable pageable
    ) {
        PageableValidator.validate(
                pageable,
                ALLOWED_SORT_FIELDS
        );

        validateSearchDates(criteria);

        Specification<Reservation> specification =
                ReservationSpecifications.withFilters(criteria);

        Page<Reservation> reservationPage =
                reservationRepository.findAll(
                        specification,
                        pageable
                );

        return PageResponse.from(
                reservationPage,
                reservationMapper::toResponse
        );
    }

    private void validateSearchDates(
            ReservationSearchCriteria criteria
    ) {
        LocalDate checkIn = criteria.checkIn();
        LocalDate checkOut = criteria.checkOut();

        if (checkIn != null
                && checkOut != null
                && !checkOut.isAfter(checkIn)) {

            throw new BusinessValidationException(
                    "Search check-out date must be after check-in date"
            );
        }
    }
    private Reservation findReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found with id: " + id
                        )
                );
    }

    private void validateReservationRequest(
            ReservationRequest request
    ) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new BusinessValidationException(
                    "Check-out date must be after check-in date"
            );
        }

        BigDecimal totalPrice = request.totalPrice();

        if (totalPrice.signum() < 0) {
            throw new BusinessValidationException(
                    "Total price cannot be negative"
            );
        }
    }
}
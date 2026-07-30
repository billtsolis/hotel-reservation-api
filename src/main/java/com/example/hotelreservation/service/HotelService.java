package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.common.PageResponse;
import com.example.hotelreservation.dto.hotel.HotelRequest;
import com.example.hotelreservation.dto.hotel.HotelResponse;
import com.example.hotelreservation.entity.Hotel;
import com.example.hotelreservation.exception.BusinessValidationException;
import com.example.hotelreservation.exception.ResourceInUseException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.HotelRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.util.PageableValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Slf4j
public class HotelService {
    private final HotelRepository hotelRepository;
    private final ReservationRepository reservationRepository;
    private final HotelMapper hotelMapper;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "city", "stars");

    public HotelService(HotelRepository hotelRepository,ReservationRepository reservationRepository, HotelMapper hotelMapper){
        this.hotelRepository = hotelRepository;
        this.reservationRepository = reservationRepository;
        this.hotelMapper = hotelMapper;
    }

    private void validateStars(Integer stars){
        if (stars == null || stars < 1 || stars > 5) {
            throw new BusinessValidationException(
                    "Hotel stars must be between 1 and 5"
            );
        }

    }
    private Hotel findHotelById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hotel not found with id: " + id
                        )
                );
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request){
        log.info("Creating hotel name={} city={} stars={}", request.name(), request.city(), request.stars());
        validateStars(request.stars());

        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel created successfully id={}", savedHotel.getId());

        return hotelMapper.toResponse(savedHotel);
    }

    @Transactional(readOnly = true)
    public PageResponse<HotelResponse> getAllHotels(Pageable pageable) {
        PageableValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        Page<Hotel> hotelPage = hotelRepository.findAll(pageable);

        return PageResponse.from(hotelPage, hotelMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long id){
        Hotel hotel = findHotelById(id);
        return hotelMapper.toResponse(hotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, HotelRequest request){
        log.info("Updating hotel id={}", id);
        validateStars(request.stars());

        Hotel hotel = findHotelById((id));
        hotelMapper.updateEntity(hotel, request);
        Hotel updateHotel = hotelRepository.save(hotel);
        log.info("Hotel updated successfully id={}", id);

        return hotelMapper.toResponse(updateHotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("Soft deleting hotel id={}", id);

        Hotel hotel = findHotelById(id);

        if (reservationRepository.existsByHotel_Id(id)) {
            log.warn(
                    "Hotel deletion rejected because reservations exist id={}",
                    id
            );

            throw new ResourceInUseException(
                    "Hotel cannot be deleted because it has reservations"
            );
        }

        hotelRepository.delete(hotel);
        log.info("Hotel soft deleted successfully id={}", id);
    }
}

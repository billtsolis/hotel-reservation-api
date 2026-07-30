package com.example.hotelreservation.specification;

import com.example.hotelreservation.dto.reservation.ReservationSearchCriteria;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Hotel;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.ReservationStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<Reservation> withFilters(
            ReservationSearchCriteria criteria
    ) {
        Specification<Reservation> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (hasText(criteria.hotelName())) {
            specification = specification.and(
                    hotelNameContains(criteria.hotelName())
            );
        }

        if (hasText(criteria.customerName())) {
            specification = specification.and(
                    customerNameContains(criteria.customerName())
            );
        }

        if (hasText(criteria.city())) {
            specification = specification.and(
                    cityContains(criteria.city())
            );
        }

        if (criteria.status() != null) {
            specification = specification.and(
                    hasStatus(criteria.status())
            );
        }

        if (criteria.checkIn() != null) {
            specification
                    = specification.and(
                    checkInFrom(criteria.checkIn())
            );
        }

        if (criteria.checkOut() != null) {
            specification = specification.and(
                    checkOutUntil(criteria.checkOut())
            );
        }

        return specification;
    }

    private static Specification<Reservation> hotelNameContains(
            String hotelName
    ) {
        return (root, query, criteriaBuilder) -> {
            Join<Reservation, Hotel> hotel =
                    root.join("hotel", JoinType.INNER);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(hotel.get("name")),
                    containsPattern(hotelName)
            );
        };
    }

    private static Specification<Reservation> cityContains(
            String city
    ) {
        return (root, query, criteriaBuilder) -> {
            Join<Reservation, Hotel> hotel =
                    root.join("hotel", JoinType.INNER);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(hotel.get("city")),
                    containsPattern(city)
            );
        };
    }

    private static Specification<Reservation> customerNameContains(
            String customerName
    ) {
        return (root, query, criteriaBuilder) -> {
            Join<Reservation, Customer> customer =
                    root.join("customer", JoinType.INNER);

            Expression<String> firstName =
                    criteriaBuilder.lower(
                            customer.get("firstName")
                    );

            Expression<String> lastName =
                    criteriaBuilder.lower(
                            customer.get("lastName")
                    );

            Expression<String> fullName =
                    criteriaBuilder.concat(
                            criteriaBuilder.concat(
                                    firstName,
                                    " "
                            ),
                            lastName
                    );

            String pattern = containsPattern(customerName);

            return criteriaBuilder.or(
                    criteriaBuilder.like(firstName, pattern),
                    criteriaBuilder.like(lastName, pattern),
                    criteriaBuilder.like(fullName, pattern)
            );
        };
    }

    private static Specification<Reservation> hasStatus(
            ReservationStatus status
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    private static Specification<Reservation> checkInFrom(
            LocalDate checkIn
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.<LocalDate>get("checkIn"),
                        checkIn
                );
    }

    private static Specification<Reservation> checkOutUntil(
            LocalDate checkOut
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.<LocalDate>get("checkOut"),
                        checkOut
                );
    }

    private static String containsPattern(String value) {
        return "%"
                + value.trim().toLowerCase(Locale.ROOT)
                + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

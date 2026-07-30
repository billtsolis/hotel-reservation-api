package com.example.hotelreservation.util;

import com.example.hotelreservation.exception.BusinessValidationException;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public final class PageableValidator {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableValidator() {
    }

    public static void validate(
            Pageable pageable,
            Set<String> allowedSortFields
    ) {
        if (pageable == null) {
            throw new BusinessValidationException(
                    "Pagination information is required"
            );
        }

        if (pageable.isUnpaged()) {
            return;
        }

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BusinessValidationException(
                    "Page size cannot exceed " + MAX_PAGE_SIZE
            );
        }

        pageable.getSort().forEach(order -> {
            String property = order.getProperty();

            if (!allowedSortFields.contains(property)) {
                throw new BusinessValidationException(
                        "Invalid sorting field: " + property
                );
            }
        });
    }
}
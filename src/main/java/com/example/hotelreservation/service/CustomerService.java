package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.common.PageResponse;
import com.example.hotelreservation.dto.customer.CustomerRequest;
import com.example.hotelreservation.dto.customer.CustomerResponse;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.exception.DuplicateEmailException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.CustomerMapper;
import com.example.hotelreservation.repository.CustomerRepository;
import com.example.hotelreservation.util.PageableValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Slf4j
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "firstName", "lastName", "email");

    public CustomerService (CustomerRepository customerRepository, CustomerMapper customerMapper){
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse createCustomer(
            CustomerRequest request
    ) {
        log.info("Creating customer");
        String normalizedEmail = normalizeEmail(request.email());

        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Customer creation rejected due to duplicate email");

            throw new DuplicateEmailException(
                    "Customer email already exists: " + normalizedEmail
            );
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setEmail(normalizedEmail);

        try {
            Customer savedCustomer = customerRepository.saveAndFlush(customer);
            log.info("Customer created successfully id={}", savedCustomer.getId());
            return customerMapper.toResponse(savedCustomer);

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException("Customer email already exists: " + normalizedEmail);
        }
    }


    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        PageableValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        Page<Customer> customerPage = customerRepository.findAll(pageable);

        return PageResponse.from(customerPage, customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById (Long id) {
        Customer customer = findCustomerById(id);
        return customerMapper.toResponse(customer);
    }

    private Customer findCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id: " + id
                        )
                );
    }
    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

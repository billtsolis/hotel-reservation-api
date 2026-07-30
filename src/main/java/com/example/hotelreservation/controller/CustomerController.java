package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.common.PageResponse;
import com.example.hotelreservation.dto.customer.CustomerRequest;
import com.example.hotelreservation.dto.customer.CustomerResponse;
import com.example.hotelreservation.service.CustomerService;
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
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(
            CustomerService customerService
    ) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request
    ) {
        CustomerResponse createdCustomer =
                customerService.createCustomer(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCustomer.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdCustomer);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        PageResponse<CustomerResponse> response = customerService.getAllCustomers(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Long id
    ) {
        CustomerResponse response = customerService.getCustomerById(id);

        return ResponseEntity.ok(response);
    }
}
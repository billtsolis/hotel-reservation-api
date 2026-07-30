package com.example.hotelreservation.mapper;


import com.example.hotelreservation.dto.customer.CustomerRequest;
import com.example.hotelreservation.dto.customer.CustomerResponse;
import com.example.hotelreservation.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    public Customer toEntity(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(request.email().trim().toLowerCase());
        return customer;
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail()
        );
    }
}

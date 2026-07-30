package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.customer.CustomerRequest;
import com.example.hotelreservation.dto.customer.CustomerResponse;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.exception.DuplicateEmailException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.CustomerMapper;
import com.example.hotelreservation.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createCustomer_shouldSaveCustomerAndNormalizeEmail() {
        CustomerRequest request = new CustomerRequest(
                "John",
                "Smith",
                "  JOHN@EXAMPLE.COM  "
        );

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Smith");

        Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setFirstName("John");
        savedCustomer.setLastName("Smith");
        savedCustomer.setEmail("john@example.com");

        CustomerResponse expectedResponse = new CustomerResponse(
                1L,
                "John",
                "Smith",
                "john@example.com"
        );

        when(customerRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);

        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.createCustomer(request);

        assertEquals(expectedResponse, result);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);

        verify(customerRepository).saveAndFlush(customerCaptor.capture());

        Customer customerPassedToRepository = customerCaptor.getValue();

        assertEquals("john@example.com", customerPassedToRepository.getEmail());

        verify(customerRepository)
                .existsByEmailIgnoreCase("john@example.com");
    }

    @Test
    void createCustomer_shouldRejectDuplicateEmail() {
        CustomerRequest request = new CustomerRequest(
                "John",
                "Smith",
                "john@example.com"
        );

        when(customerRepository.existsByEmailIgnoreCase(
                "john@example.com"
        )).thenReturn(true);

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> customerService.createCustomer(request)
        );

        assertEquals(
                "Customer email already exists: john@example.com",
                exception.getMessage()
        );

        verify(customerRepository)
                .existsByEmailIgnoreCase("john@example.com");

        verify(customerRepository, never()).save(any());
        verifyNoInteractions(customerMapper);
    }

    @Test
    void createCustomer_shouldHandleDatabaseDuplicateConstraint() {
        CustomerRequest request = new CustomerRequest(
                "John",
                "Smith",
                "john@example.com"
        );

        Customer customer = new Customer();

        when(customerRepository.existsByEmailIgnoreCase(
                "john@example.com"
        )).thenReturn(false);

        when(customerMapper.toEntity(request))
                .thenReturn(customer);

        when(customerRepository.saveAndFlush(customer))
                .thenThrow(new DataIntegrityViolationException(
                        "Unique constraint violation"
                ));

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> customerService.createCustomer(request)
        );

        assertEquals(
                "Customer email already exists: john@example.com",
                exception.getMessage()
        );
    }

    @Test
    void getCustomerById_shouldReturnCustomerWhenFound() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("John");
        customer.setLastName("Smith");
        customer.setEmail("john@example.com");

        CustomerResponse response = new CustomerResponse(
                1L,
                "John",
                "Smith",
                "john@example.com"
        );

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(customerMapper.toResponse(customer))
                .thenReturn(response);

        CustomerResponse result =
                customerService.getCustomerById(1L);

        assertEquals(response, result);

        verify(customerRepository).findById(1L);
        verify(customerMapper).toResponse(customer);
    }

    @Test
    void getCustomerById_shouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> customerService.getCustomerById(99L)
        );

        assertEquals(
                "Customer not found with id: 99",
                exception.getMessage()
        );

        verify(customerRepository).findById(99L);
        verifyNoInteractions(customerMapper);
    }
}
package com.example.hotelreservation;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
@SpringBootTest(properties = {
        "app.security.jwt-secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.jwt-expiration-seconds=3600"
})
class HotelReservationApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("hotel_reservation_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Test
    void contextLoads() {
    }
}
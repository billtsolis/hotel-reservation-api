package com.example.hotelreservation.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        "app.security.jwt-secret=integration-test-jwt-secret-with-more-than-32-characters",
        "app.security.jwt-expiration-seconds=3600"
})
@AutoConfigureMockMvc
class HotelReservationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("hotel_reservation_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM customers");
        jdbcTemplate.update("DELETE FROM hotels");
    }

    //1st test flyway
    @Test
    void flywayMigrationsShouldBeApplied() {

        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = true
                """,
                Integer.class
        );

        assertThat(migrationCount)
                .isNotNull()
                .isGreaterThanOrEqualTo(3);
    }

    //2nd test security
    @Test
    void protectedEndpointWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/hotels")).andExpect(status().isUnauthorized());
    }

    //3rd test end-to-end test
    @Test
    void shouldCreateHotelCustomerAndReservation() throws Exception {
        String token = loginAndGetToken();

        // 1. Create Hotel
        MvcResult hotelResult = mockMvc.perform(
                        post("/hotels")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "name": "Hilton Integration Test",
                                      "city": "Athens",
                                      "stars": 5
                                    }
                                    """)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(
                        jsonPath("$.name")
                                .value("Hilton Integration Test")
                )
                .andExpect(
                        jsonPath("$.city")
                                .value("Athens")
                )
                .andExpect(
                        jsonPath("$.stars")
                                .value(5)
                )
                .andReturn();


        Number hotelId = JsonPath.read(
                hotelResult
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        // 2. Create Customer
        MvcResult customerResult = mockMvc.perform(
                        post("/customers")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "firstName": "John",
                                      "lastName": "Integration",
                                      "email": "john.integration@example.com"
                                    }
                                    """)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(
                        jsonPath("$.firstName")
                                .value("John")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("Integration")
                )
                .andReturn();


        Number customerId = JsonPath.read(
                customerResult
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        // 3. Create Reservation
        String reservationJson = """
            {
              "hotelId": %d,
              "customerId": %d,
              "checkIn": "2026-09-10",
              "checkOut": "2026-09-15",
              "totalPrice": 750.00
            }
            """.formatted(
                hotelId.longValue(),
                customerId.longValue()
        );

        MvcResult reservationResult = mockMvc.perform(
                        post("/reservations")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content(reservationJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.totalPrice")
                                .value(750.00)
                )
                .andReturn();


        Number reservationId = JsonPath.read(
                reservationResult
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );

        // 4. Search Reservations
        mockMvc.perform(
                        get("/reservations/search")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .param("city", "Athens")
                                .param("status", "ACTIVE")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        // 5. Verify directly in PostgreSQL

        Integer reservationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM reservations
                        WHERE id = ?
                          AND status = 'ACTIVE'
                        """,
                        Integer.class,
                        reservationId.longValue()
                );


        assertThat(reservationCount)
                .isEqualTo(1);
    }

    //5th test logical cancellation
    @Test
    void deleteReservationShouldPerformLogicalCancellation()
            throws Exception {

        String token = loginAndGetToken();


        // Hotel
        MvcResult hotelResult = mockMvc.perform(
                        post("/hotels")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "name": "Cancellation Hotel",
                                      "city": "Athens",
                                      "stars": 4
                                    }
                                    """)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();


        Number hotelId = JsonPath.read(
                hotelResult.getResponse().getContentAsString(),
                "$.id"
        );


        // Customer
        MvcResult customerResult = mockMvc.perform(
                        post("/customers")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "firstName": "Maria",
                                      "lastName": "Test",
                                      "email": "maria.integration@example.com"
                                    }
                                    """)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();


        Number customerId = JsonPath.read(
                customerResult.getResponse().getContentAsString(),
                "$.id"
        );


        // Reservation
        MvcResult reservationResult = mockMvc.perform(
                        post("/reservations")
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "hotelId": %d,
                                      "customerId": %d,
                                      "checkIn": "2026-10-10",
                                      "checkOut": "2026-10-15",
                                      "totalPrice": 500.00
                                    }
                                    """.formatted(
                                        hotelId.longValue(),
                                        customerId.longValue()
                                ))
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();


        Number reservationId = JsonPath.read(
                reservationResult.getResponse().getContentAsString(),
                "$.id"
        );


        // Cancel
        mockMvc.perform(
                        delete(
                                "/reservations/{id}",
                                reservationId.longValue()
                        )
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().is2xxSuccessful());


        // Verify row still exists, but CANCELLED
        String status = jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM reservations
                WHERE id = ?
                """,
                String.class,
                reservationId.longValue()
        );


        assertThat(status)
                .isEqualTo("CANCELLED");
    }

    //Helper for login
    private String loginAndGetToken() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "username": "admin",
                                      "password": "admin"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String responseBody =
                result.getResponse().getContentAsString();

        return JsonPath.read(
                responseBody,
                "$.accessToken"
        );
    }
}
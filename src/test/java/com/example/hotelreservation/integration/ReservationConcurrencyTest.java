package com.example.hotelreservation.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.security.jwt-secret=integration-test-jwt-secret-with-more-than-32-characters",
                "app.security.jwt-expiration-seconds=3600"
        }
)
class ReservationConcurrencyTest {
    //ReservationConcurrencyIntegrationTest
// concurrentOverlappingReservationsShouldAllowExactlyOne
// databaseConstraintShouldPreventConcurrentOverlap
// adjacentReservationsShouldBeAllowed
// cancelledReservationShouldNotBlockNewReservation

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("hotel_reservation_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {

        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM customers");
        jdbcTemplate.update("DELETE FROM hotels");

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    //Helper for base URL
    private String baseUrl() {
        return "http://localhost:" + port;
    }

    //Helper για JWT login
    private String loginAndGetToken() throws Exception {

        String body = """
            {
              "username": "admin",
              "password": "admin"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertThat(response.statusCode())
                .isEqualTo(200);

        return JsonPath.read(
                response.body(),
                "$.accessToken"
        );
    }

    //Helpers for Hotel and Customer
    private long createHotel(String token) throws Exception {

        String body = """
            {
              "name": "Concurrency Hotel",
              "city": "Athens",
              "stars": 5
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/hotels"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertThat(response.statusCode())
                .isBetween(200, 299);

        Number id = JsonPath.read(
                response.body(),
                "$.id"
        );

        return id.longValue();
    }

    private long createCustomer(String token) throws Exception {

        String email =
                "concurrency." +
                        System.nanoTime() +
                        "@example.com";

        String body = """
            {
              "firstName": "Concurrent",
              "lastName": "Customer",
              "email": "%s"
            }
            """.formatted(email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/customers"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertThat(response.statusCode())
                .isBetween(200, 299);

        Number id = JsonPath.read(
                response.body(),
                "$.id"
        );

        return id.longValue();
    }

    //Helper for Reservation request
    private HttpRequest reservationRequest(
            String token,
            long hotelId,
            long customerId,
            LocalDate checkIn,
            LocalDate checkOut
    ) {

        String body = """
            {
              "hotelId": %d,
              "customerId": %d,
              "checkIn": "%s",
              "checkOut": "%s",
              "totalPrice": 500.00
            }
            """.formatted(
                hotelId,
                customerId,
                checkIn,
                checkOut
        );

        return HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                baseUrl() + "/reservations"
                        )
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(body)
                )
                .build();
    }

    //basic concurrency test
    @Test
    void concurrentOverlappingReservationsShouldAllowExactlyOne()
            throws Exception {

        String token = loginAndGetToken();

        long hotelId = createHotel(token);
        long customerId = createCustomer(token);

        LocalDate firstCheckIn =
                LocalDate.of(2026, 10, 10);

        LocalDate firstCheckOut =
                LocalDate.of(2026, 10, 15);

        LocalDate secondCheckIn =
                LocalDate.of(2026, 10, 12);

        LocalDate secondCheckOut =
                LocalDate.of(2026, 10, 18);


        HttpRequest request1 =
                reservationRequest(
                        token,
                        hotelId,
                        customerId,
                        firstCheckIn,
                        firstCheckOut
                );

        HttpRequest request2 =
                reservationRequest(
                        token,
                        hotelId,
                        customerId,
                        secondCheckIn,
                        secondCheckOut
                );


        CompletableFuture<HttpResponse<String>> future1 =
                httpClient.sendAsync(
                        request1,
                        HttpResponse.BodyHandlers.ofString()
                );

        CompletableFuture<HttpResponse<String>> future2 =
                httpClient.sendAsync(
                        request2,
                        HttpResponse.BodyHandlers.ofString()
                );


        HttpResponse<String> response1 =
                future1.join();

        HttpResponse<String> response2 =
                future2.join();


        System.out.println("=================================");
        System.out.println("Response 1 status: " + response1.statusCode());
        System.out.println("Response 1 body: " + response1.body());

        System.out.println("---------------------------------");

        System.out.println("Response 2 status: " + response2.statusCode());
        System.out.println("Response 2 body: " + response2.body());
        System.out.println("=================================");

        List<Integer> statuses =
                List.of(
                        response1.statusCode(),
                        response2.statusCode()
                );


        long successCount = statuses.stream()
                .filter(
                        status ->
                                status >= 200 &&
                                        status < 300
                )
                .count();


        long conflictCount = statuses.stream()
                .filter(status -> status == 409)
                .count();


        assertThat(successCount)
                .withFailMessage(
                        """
                        Expected exactly one successful reservation.
        
                        Response 1:
                        Status: %d
                        Body: %s
        
                        Response 2:
                        Status: %d
                        Body: %s
                        """,
                        response1.statusCode(),
                        response1.body(),
                        response2.statusCode(),
                        response2.body()
                )
                .isEqualTo(1);

        assertThat(conflictCount)
                .withFailMessage(
                        """
                        Expected exactly one 409 Conflict.
        
                        Response 1:
                        Status: %d
                        Body: %s
        
                        Response 2:
                        Status: %d
                        Body: %s
                        """,
                        response1.statusCode(),
                        response1.body(),
                        response2.statusCode(),
                        response2.body()
                )
                .isEqualTo(1);


        Integer databaseCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM reservations
                        WHERE customer_id = ?
                          AND status = 'ACTIVE'
                        """,
                        Integer.class,
                        customerId
                );


        assertThat(databaseCount)
                .isEqualTo(1);
    }

    //Helper for instant DB insertion
    private void insertReservationDirectly(
            Connection connection,
            long hotelId,
            long customerId,
            LocalDate checkIn,
            LocalDate checkOut
    ) throws SQLException {

        String sql = """
            INSERT INTO reservations (
                hotel_id,
                customer_id,
                check_in,
                check_out,
                total_price,
                status,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, 'ACTIVE',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP)
            """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    hotelId
            );

            statement.setLong(
                    2,
                    customerId
            );

            statement.setDate(
                    3,
                    Date.valueOf(checkIn)
            );

            statement.setDate(
                    4,
                    Date.valueOf(checkOut)
            );

            statement.setBigDecimal(
                    5,
                    new BigDecimal("500.00")
            );

            statement.executeUpdate();
        }
    }

    //DB-level concurrency test
    @Test
    void databaseConstraintShouldPreventConcurrentOverlap()
            throws Exception {

        Long hotelId = jdbcTemplate.queryForObject(
                """
                INSERT INTO hotels (
                    name,
                    city,
                    stars,
                    deleted,
                    created_at,
                    updated_at
                )
                VALUES (
                    'DB Constraint Hotel',
                    'Athens',
                    5,
                    false,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class
        );


        Long customerId = jdbcTemplate.queryForObject(
                """
                INSERT INTO customers (
                    first_name,
                    last_name,
                    email,
                    created_at,
                    updated_at
                )
                VALUES (
                    'Database',
                    'Concurrency',
                    'database-concurrency@example.com',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """,
                Long.class
        );


        CountDownLatch startLatch =
                new CountDownLatch(1);


        ExecutorService executor =
                Executors.newFixedThreadPool(2);


        try {

            CompletableFuture<Boolean> first =
                    CompletableFuture.supplyAsync(
                            () -> attemptDatabaseReservation(
                                    startLatch,
                                    hotelId,
                                    customerId,
                                    LocalDate.of(
                                            2026,
                                            11,
                                            10
                                    ),
                                    LocalDate.of(
                                            2026,
                                            11,
                                            15
                                    )
                            ),
                            executor
                    );


            CompletableFuture<Boolean> second =
                    CompletableFuture.supplyAsync(
                            () -> attemptDatabaseReservation(
                                    startLatch,
                                    hotelId,
                                    customerId,
                                    LocalDate.of(
                                            2026,
                                            11,
                                            12
                                    ),
                                    LocalDate.of(
                                            2026,
                                            11,
                                            18
                                    )
                            ),
                            executor
                    );


            startLatch.countDown();


            boolean firstSucceeded =
                    first.join();

            boolean secondSucceeded =
                    second.join();


            assertThat(
                    List.of(
                            firstSucceeded,
                            secondSucceeded
                    )
            )
                    .containsExactlyInAnyOrder(
                            true,
                            false
                    );


            Integer count =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM reservations
                            WHERE customer_id = ?
                              AND status = 'ACTIVE'
                            """,
                            Integer.class,
                            customerId
                    );


            assertThat(count)
                    .isEqualTo(1);

        } finally {

            executor.shutdownNow();
        }
    }

    //Helper attemptDatabaseReservation
    private boolean attemptDatabaseReservation(
            CountDownLatch startLatch,
            long hotelId,
            long customerId,
            LocalDate checkIn,
            LocalDate checkOut
    ) {

        try (
                Connection connection =
                        dataSource.getConnection()
        ) {

            connection.setAutoCommit(false);

            startLatch.await();

            try {

                insertReservationDirectly(
                        connection,
                        hotelId,
                        customerId,
                        checkIn,
                        checkOut
                );

                connection.commit();

                return true;

            } catch (SQLException exception) {

                connection.rollback();

                assertThat(
                        exception.getSQLState()
                ).isEqualTo("23P01");

                return false;
            }

        } catch (Exception exception) {

            throw new RuntimeException(exception);
        }
    }

    //Test for boundary
    @Test
    void adjacentReservationsShouldBeAllowed()
            throws Exception {

        String token = loginAndGetToken();

        long hotelId =
                createHotel(token);

        long customerId =
                createCustomer(token);


        HttpResponse<String> first =
                httpClient.send(
                        reservationRequest(
                                token,
                                hotelId,
                                customerId,
                                LocalDate.of(2026, 12, 10),
                                LocalDate.of(2026, 12, 15)
                        ),
                        HttpResponse.BodyHandlers.ofString()
                );


        HttpResponse<String> second =
                httpClient.send(
                        reservationRequest(
                                token,
                                hotelId,
                                customerId,
                                LocalDate.of(2026, 12, 15),
                                LocalDate.of(2026, 12, 20)
                        ),
                        HttpResponse.BodyHandlers.ofString()
                );


        assertThat(first.statusCode())
                .isBetween(200, 299);

        assertThat(second.statusCode())
                .isBetween(200, 299);


        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM reservations
                        WHERE customer_id = ?
                          AND status = 'ACTIVE'
                        """,
                        Integer.class,
                        customerId
                );


        assertThat(count)
                .isEqualTo(2);
    }

    //Test for CANCELLED
    @Test
    void cancelledReservationShouldNotBlockNewReservation()
            throws Exception {

        String token = loginAndGetToken();

        long hotelId =
                createHotel(token);

        long customerId =
                createCustomer(token);


        HttpResponse<String> first =
                httpClient.send(
                        reservationRequest(
                                token,
                                hotelId,
                                customerId,
                                LocalDate.of(2027, 1, 10),
                                LocalDate.of(2027, 1, 15)
                        ),
                        HttpResponse.BodyHandlers.ofString()
                );


        assertThat(first.statusCode())
                .isBetween(200, 299);


        Number reservationId =
                JsonPath.read(
                        first.body(),
                        "$.id"
                );


        HttpRequest cancelRequest =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        baseUrl()
                                                + "/reservations/"
                                                + reservationId.longValue()
                                )
                        )
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .DELETE()
                        .build();


        HttpResponse<String> cancellation =
                httpClient.send(
                        cancelRequest,
                        HttpResponse.BodyHandlers.ofString()
                );


        assertThat(cancellation.statusCode())
                .isBetween(200, 299);


        HttpResponse<String> replacement =
                httpClient.send(
                        reservationRequest(
                                token,
                                hotelId,
                                customerId,
                                LocalDate.of(2027, 1, 12),
                                LocalDate.of(2027, 1, 18)
                        ),
                        HttpResponse.BodyHandlers.ofString()
                );


        assertThat(replacement.statusCode())
                .isBetween(200, 299);
    }
}
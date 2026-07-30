# Hotel Reservation Management API

A Spring Boot REST API for managing hotels, customers, and hotel reservations.

The application was developed as part of the Marine Tours Mid-Level Java & SQL Developer assessment.

## Features

The API supports:

- Creating, retrieving, updating, and deleting hotels
- Creating and retrieving customers
- Creating and retrieving reservations
- Logical cancellation of reservations
- Dynamic reservation search using JPA Specifications
- Pagination and sorting
- Validation of business rules
- Global exception handling
- PostgreSQL database migrations with Flyway
- Swagger/OpenAPI documentation
- JWT authentication
- Audit timestamps
- Logging
- Hotel soft deletion
- Unit tests with JUnit 5 and Mockito

## Technology Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT authentication
- Maven
- PostgreSQL
- Flyway
- Lombok
- JUnit 5
- Mockito
- Docker Compose
- Swagger/OpenAPI

## Requirements

Before running the application, make sure the following tools are installed:

- Java 21 or later
- Docker and Docker Compose
- Git

Maven does not need to be installed separately because the project includes the Maven Wrapper.

Verify the Java version:

```bash
java -version
```

The output should indicate Java 21 or later.

## Project Structure

```text
src
├── main
│   ├── java/com/example/hotelreservation
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── exception
│   │   ├── mapper
│   │   ├── repository
│   │   ├── security
│   │   ├── service
│   │   ├── specification
│   │   └── util
│   └── resources
│       ├── db/migration
│       └── application.yml
└── test
    └── java/com/example/hotelreservation/service
```

## Database Configuration

The application uses PostgreSQL.

Default database configuration:

```text
Host: localhost
Port: 5432
Database: hotel_reservation
Username: hotel_user
Password: hotel_password
```

The datasource configuration supports environment variables:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/hotel_reservation}
    username: ${DB_USERNAME:hotel_user}
    password: ${DB_PASSWORD:hotel_password}
```

Supported variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

## Starting PostgreSQL

From the project root directory, run:

```bash
docker compose up -d
```

Verify that the container is running:

```bash
docker compose ps
```

View database logs:

```bash
docker compose logs postgres
```

Stop the database:

```bash
docker compose down
```

Stop the database and delete its stored data:

```bash
docker compose down -v
```

## Database Migrations

Flyway manages the database schema.

Migration files are located in:

```text
src/main/resources/db/migration
```

Current migrations include:

```text
V1__create_initial_schema.sql
V2__add_audit_and_soft_delete.sql
```

The migrations run automatically when the application starts.

Hibernate is configured to validate the schema:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Hibernate validates the schema but does not create or modify tables.

## Security Configuration

The API supports JWT authentication.

JWT configuration:

```yaml
app:
  security:
    jwt-secret: ${JWT_SECRET:hotel-reservation-assessment-secret-key-2026}
    jwt-expiration-seconds: ${JWT_EXPIRATION_SECONDS:3600}
```

Local development credentials:

```text
Username: admin
Password: admin
```

Production secrets should be provided through environment variables and must not be committed to Git.

Recommended variables:

```text
JWT_SECRET
JWT_EXPIRATION_SECONDS
```

The JWT secret must contain at least 32 bytes.

## Running the Application

First, start PostgreSQL:

```bash
docker compose up -d
```

### Windows

```bash
mvnw.cmd spring-boot:run
```

### macOS or Linux

```bash
./mvnw spring-boot:run
```

The application starts by default at:

```text
http://localhost:8080
```

The application can also be started from IntelliJ IDEA by running:

```text
HotelReservationApplication.java
```

## Running Tests

The project contains service-layer unit tests using JUnit 5 and Mockito.

The tests cover:

- Hotel validation
- Customer email validation
- Duplicate email handling
- Reservation date validation
- Negative reservation price validation
- Reservation overlap validation
- Missing hotel and customer scenarios
- Logical reservation cancellation
- Hotel deletion validation

### Windows

```bash
mvnw.cmd test
```

### macOS or Linux

```bash
./mvnw test
```

To clean the project and execute all tests:

### Windows

```bash
mvnw.cmd clean test
```

### macOS or Linux

```bash
./mvnw clean test
```

Tests can also be executed from IntelliJ IDEA by right-clicking the `src/test` directory and selecting:

```text
Run 'All Tests'
```

## Authentication

### Login

```http
POST /auth/login
Content-Type: application/json
```

Request:

```json
{
  "username": "admin",
  "password": "admin"
}
```

Example response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Use the returned token in protected requests:

```http
Authorization: Bearer <accessToken>
```

## API Endpoints

### Hotels

```text
POST   /hotels
GET    /hotels
GET    /hotels/{id}
PUT    /hotels/{id}
DELETE /hotels/{id}
```

### Customers

```text
POST /customers
GET  /customers
GET  /customers/{id}
```

### Reservations

```text
POST   /reservations
GET    /reservations
GET    /reservations/{id}
DELETE /reservations/{id}
GET    /reservations/search
```

The reservation `DELETE` endpoint performs logical cancellation by changing the reservation status from `ACTIVE` to `CANCELLED`.

## API Examples

All protected examples require:

```http
Authorization: Bearer <accessToken>
```

### Create a Hotel

```http
POST /hotels
Content-Type: application/json
```

```json
{
  "name": "Hilton Athens",
  "city": "Athens",
  "stars": 5
}
```

### Get Hotels with Pagination and Sorting

```http
GET /hotels?page=0&size=10&sort=stars,desc
```

### Get a Hotel

```http
GET /hotels/1
```

### Update a Hotel

```http
PUT /hotels/1
Content-Type: application/json
```

```json
{
  "name": "Hilton Athens Updated",
  "city": "Athens",
  "stars": 5
}
```

### Delete a Hotel

```http
DELETE /hotels/1
```

Hotel deletion is implemented as soft deletion. A hotel with existing reservations cannot be deleted.

### Create a Customer

```http
POST /customers
Content-Type: application/json
```

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "email": "john.smith@example.com"
}
```

Customer emails are normalized to lowercase and must be unique.

### Get Customers

```http
GET /customers?page=0&size=10&sort=lastName,asc
```

### Create a Reservation

```http
POST /reservations
Content-Type: application/json
```

```json
{
  "hotelId": 1,
  "customerId": 1,
  "checkIn": "2026-08-10",
  "checkOut": "2026-08-15",
  "totalPrice": 750.00
}
```

### Get Reservations

```http
GET /reservations?page=0&size=10&sort=checkIn,desc
```

### Cancel a Reservation

```http
DELETE /reservations/1
```

The reservation remains in the database with:

```text
status = CANCELLED
```

## Dynamic Reservation Search

Reservation search is implemented using Spring Data JPA Specifications.

Only filters supplied by the client are added to the generated query.

Supported parameters:

```text
hotelName
customerName
city
status
checkIn
checkOut
page
size
sort
```

### Search by Hotel Name

```http
GET /reservations/search?hotelName=hilton
```

### Search by Customer Name

```http
GET /reservations/search?customerName=john%20smith
```

The customer-name filter searches first name, last name, and full name.

### Search by City and Status

```http
GET /reservations/search?city=Athens&status=ACTIVE
```

Valid statuses:

```text
ACTIVE
CANCELLED
```

### Search by Date Range

```http
GET /reservations/search?checkIn=2026-08-01&checkOut=2026-08-31
```

### Search with Pagination and Sorting

```http
GET /reservations/search?city=Athens&status=ACTIVE&page=0&size=10&sort=totalPrice,desc
```

### Combined Search

```http
GET /reservations/search?hotelName=hilton&customerName=smith&city=athens&status=ACTIVE&checkIn=2026-08-01&checkOut=2026-08-31&page=0&size=10&sort=checkIn,asc
```

## Business Rules

### Hotel Stars

Hotel stars must be between `1` and `5`.

The rule is enforced through:

- Request validation
- Service-layer validation
- Database check constraint

### Customer Email

Customer email must be unique.

Email uniqueness is:

- Case-insensitive
- Checked in the service layer
- Enforced through a PostgreSQL unique index

### Reservation Dates

The check-out date must be after the check-in date. Equal check-in and check-out dates are invalid.

### Reservation Price

The total price cannot be negative. A price of zero is allowed.

### Reservation Overlap

A customer cannot have two overlapping active reservations.

Existing reservation:

```text
2026-07-10 to 2026-07-15
```

Rejected reservation:

```text
2026-07-12 to 2026-07-18
```

Overlap condition:

```text
existingCheckIn < newCheckOut
AND
existingCheckOut > newCheckIn
```

A new reservation may start on the same date that a previous reservation ends.

Cancelled reservations do not block new reservations.

## Error Handling

The application uses a global exception handler.

Errors are returned with meaningful HTTP status codes:

```text
400 Bad Request
401 Unauthorized
404 Not Found
409 Conflict
500 Internal Server Error
```

Example validation response:

```json
{
  "timestamp": "2026-07-30T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/hotels",
  "fieldErrors": {
    "stars": "Hotel stars must be between 1 and 5"
  }
}
```

## Swagger/OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Swagger endpoints are public. Protected API endpoints require a valid Bearer token.

## Audit Timestamps

The entities contain:

```text
created_at
updated_at
```

The values are populated automatically using Spring Data JPA auditing.

## Soft Delete

Hotel deletion is implemented through soft deletion:

```text
deleted = true
```

Soft-deleted hotels are excluded from normal JPA queries.

Reservations are not soft-deleted. Reservation deletion performs logical cancellation:

```text
status = CANCELLED
```

## Logging

The application logs important business operations, including:

- Hotel creation, update, and deletion
- Customer creation
- Duplicate email attempts
- Reservation creation
- Reservation overlap rejection
- Reservation cancellation
- Unexpected application errors

Passwords, JWT tokens, and JWT secrets are not logged.

## SQL Challenge

The SQL challenge queries are provided in:

```text
sql/report_queries.sql
```

The file contains:

1. Active reservation count and revenue per hotel
2. Customers who have never booked
3. The five hotels with the highest active-reservation revenue

## Assumptions

1. Java 21 is used, satisfying the Java 17+ requirement.
2. PostgreSQL is the selected SQL database.
3. IDs are generated automatically by PostgreSQL identity columns.
4. New reservations are created with status `ACTIVE`.
5. Clients cannot directly create a reservation with status `CANCELLED`.
6. Deleting a reservation performs logical cancellation.
7. Cancelled reservations remain available for historical purposes and searches.
8. Cancelled reservations do not participate in overlap validation.
9. A reservation may start on the same date another reservation ends.
10. Customer email comparison is case-insensitive.
11. A reservation price of zero is allowed.
12. Revenue reports include only `ACTIVE` reservations.
13. Hotel deletion uses soft deletion.
14. Hotels with existing reservations cannot be deleted.
15. Customer deletion is outside the required scope.
16. Search filters are optional and combined using logical `AND`.
17. Name and city searches are case-insensitive and support partial matching.
18. Search date parameters are interpreted as lower check-in and upper check-out bounds.
19. Pagination uses zero-based page numbering.
20. The default page size is `10`.
21. The maximum accepted page size is `100`.
22. JWT authentication uses an in-memory administrator account for demonstration.
23. Registration, refresh tokens, and password reset are outside the assessment scope.
24. In a high-concurrency production system, overlap protection could be strengthened using pessimistic locking or a PostgreSQL exclusion constraint.

## Git

Initialize the repository:

```bash
git init
git add .
git commit -m "Initialize hotel reservation API"
```

Do not commit:

```text
.env
target/
.idea/
*.iml
logs/
```

## Build

### Windows

```bash
mvnw.cmd clean package
```

### macOS or Linux

```bash
./mvnw clean package
```

The generated JAR is located in:

```text
target/
```

Run it with:

```bash
java -jar target/hotel-reservation-*.jar
```

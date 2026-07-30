-- Question 1: Reservations and revenue per hotel
SELECT
    h.name AS hotel,
    COUNT(r.id) AS number_of_reservations,
    COALESCE(SUM(r.total_price), 0) AS revenue
FROM hotels h
         JOIN reservations r
              ON r.hotel_id = h.id
WHERE r.status = 'ACTIVE'
GROUP BY h.id, h.name
ORDER BY h.name;


-- Question 2: Customers who never booked
SELECT
    c.id,
    c.first_name,
    c.last_name,
    c.email
FROM customers c
         LEFT JOIN reservations r
                   ON r.customer_id = c.id
WHERE r.id IS NULL;

-- Question 3: Five hotels with the highest active revenue
SELECT
    h.name AS hotel,
    SUM(r.total_price) AS revenue
FROM hotels h
         JOIN reservations r
              ON r.hotel_id = h.id
WHERE r.status = 'ACTIVE'
GROUP BY h.id, h.name
ORDER BY revenue DESC
    LIMIT 5;
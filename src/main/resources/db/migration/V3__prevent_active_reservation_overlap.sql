CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT ex_reservations_no_overlap_customer
    EXCLUDE USING gist (
        customer_id WITH =,
        (daterange(check_in, check_out, '[)')) WITH &&
    )
    WHERE (status = 'ACTIVE');
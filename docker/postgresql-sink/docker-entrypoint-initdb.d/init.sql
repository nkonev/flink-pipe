-- this file is executed once by PostgreSQL's init mechanism
ALTER SYSTEM SET log_statement = 'all';

CREATE TABLE shipments_sink (
    shipment_id INT NOT NULL PRIMARY KEY,
    order_id INT NOT NULL,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    is_arrived BOOLEAN NOT NULL
);

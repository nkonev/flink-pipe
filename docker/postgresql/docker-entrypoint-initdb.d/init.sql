-- this file is executed once by PostgreSQL's init mechanism
CREATE TABLE shipments (
    shipment_id SERIAL NOT NULL PRIMARY KEY,
    order_id SERIAL NOT NULL,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    is_arrived BOOLEAN NOT NULL
);
ALTER SEQUENCE shipments_shipment_id_seq RESTART WITH 1001;
ALTER TABLE shipments REPLICA IDENTITY FULL;

INSERT INTO shipments
VALUES (default,10001,'Beijing','Shanghai',false),
       (default,10002,'Hangzhou','Shanghai',false),
       (default,10003,'Shanghai','Hangzhou',false);

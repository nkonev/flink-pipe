CREATE TABLE shipments_ch (
   shipment_id Int32 NOT NULL,
   order_id Int32 NOT NULL,
   origin String NOT NULL,
   destination String NOT NULL,
   is_arrived Boolean NOT NULL,
   PRIMARY KEY(shipment_id)
) ENGINE = MergeTree;

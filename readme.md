# What is it ?
It is Apache Flink with [Ververica Postgres CDC](https://github.com/ververica/flink-cdc-connectors) built as one fat|uber jar to be launched via `java -jar` as a standalone application.

The purpose is - declaratively (via Flink SQL) replicate data from PostgreSQL to any other datastores.

A docker image of PostgreSQL 16 configured by Debuzium  included to be run via `docker-compose up -d`.

Sample data is placed to the predefined by Postgres Docker `docker-entrypoint-initdb.d` directory.

Concrete example to replicate data to Elasticsearch 7 derived from [example](https://www.ververica.com/blog/how-to-guide-build-streaming-etl-for-mysql-and-postgres-based-on-flink-cdc) is in branch `elastic`.

# Build
```
./gradlew clean shadowJar
```

# Run
```
java -jar /home/nkonev/javaWorkspace/flink-pipe/build/libs/flink-pipe-0.1.0-all.jar
```

```
docker exec -it flink-pipe_clickhouse_1 clickhouse-client
SELECT 'Hello, ClickHouse!'
select * from shipments_ch;
```

Then insert some data to PostgreSQL
```
docker exec -i -t flink-pipe_postgres_1 psql -U postgres

INSERT INTO shipments VALUES (default,10004,'Moscow','Zuzino',true);
INSERT INTO shipments VALUES (default,10004,'Moscow','Zyablikovo',false), (default,10006,'Arkhangelsk','Leninskii',false);
```

# Links
* [Streaming SQL with Apache Flink: A Gentle Introduction](https://blog.rockthejvm.com/flink-sql-introduction/)
* [How-to guide: Build Streaming ETL for MySQL and Postgres based on Flink CDC](https://www.ververica.com/blog/how-to-guide-build-streaming-etl-for-mysql-and-postgres-based-on-flink-cdc)
* [Streaming ETL for MySQL and Postgres with Flink CDC](https://ververica.github.io/flink-cdc-connectors/release-3.0/content/quickstart/mysql-postgres-tutorial.html)
* [Потоковый захват изменений из PostgreSQL/MySQL с помощью Apache Flink](https://habr.com/ru/companies/neoflex/articles/567930/)
* [Как использовать Spring в качестве фреймворка для Flink-приложений](https://habr.com/ru/companies/ru_mts/articles/775970/)
* [debezium/container-images](https://github.com/debezium/container-images/tree/main/examples/postgres)

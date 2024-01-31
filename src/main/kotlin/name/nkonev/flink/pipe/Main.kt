package name.nkonev.flink.pipe

import org.apache.commons.configuration2.ConfigurationMap
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment


class Main {
    private val checkpointsDir  = "file://${System.getProperty("user.dir")}/checkpoints/"
    private val rocksDBStateDir = "file://${System.getProperty("user.dir")}/state/rocksdb/"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello there!")
            Main().runStream()
        }
    }

    fun runStream() {
        // https://commons.apache.org/proper/commons-configuration/userguide/howto_combinedbuilder.html
        val apacheCommonsConfig = CombinedConfigurationBuilder()
            .configure(Parameters().xml().setPath("config.xml"))
            .configuration

        val apacheCommonsConfigMap = ConfigurationMap(apacheCommonsConfig) as Map<String, String>

        val configuration = Configuration.fromMap(apacheCommonsConfigMap)

        val environment = StreamExecutionEnvironment
            .createLocalEnvironmentWithWebUI(configuration)

        environment.parallelism = 3

        // Checkpoint Configurations
        environment.enableCheckpointing(5000)
        environment.checkpointConfig.minPauseBetweenCheckpoints = 100
        environment.checkpointConfig.setCheckpointStorage(checkpointsDir)

        val stateBackend = EmbeddedRocksDBStateBackend()
        stateBackend.setDbStoragePath(rocksDBStateDir)
        environment.stateBackend = stateBackend

        environment.checkpointConfig.externalizedCheckpointCleanup =
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION

        // Configure Restart Strategy
        environment.restartStrategy = RestartStrategies.fixedDelayRestart(5, Time.seconds(5))

        val tableEnvironment = StreamTableEnvironment.create(environment)

        // Run some SQL queries to check the existing Catalogs, Databases and Tables
        tableEnvironment
            .executeSql("SHOW CATALOGS")
            .print()

        tableEnvironment
            .executeSql("SHOW DATABASES")
            .print()

        tableEnvironment
            .executeSql("SHOW TABLES")
            .print()

        tableEnvironment
            .executeSql("""
                            CREATE TABLE shipments (
                                shipment_id INT,
                                order_id INT,
                                origin STRING,
                                destination STRING,
                                is_arrived BOOLEAN,
                                PRIMARY KEY (shipment_id) NOT ENFORCED
                            ) WITH (
                                'connector' = 'postgres-cdc',
                                'hostname' = 'localhost',
                                'port' = '5432',
                                'username' = 'postgres',
                                'password' = 'postgres',
                                'database-name' = 'postgres',
                                'schema-name' = 'public',
                                'table-name' = 'shipments',
                                'slot.name' = 'flink'
                            );
            """.trimIndent())
            .print()


        // https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/table/print/
        tableEnvironment
            .executeSql("""
                            CREATE TABLE print_sink 
                            WITH (
                                'connector' = 'print',
                                'print-identifier' = 'DEBUG_PRINT'
                            )
                            LIKE shipments (EXCLUDING ALL);
            """.trimIndent())
            .print()


        tableEnvironment
            .executeSql("""
                            INSERT INTO print_sink
                            SELECT s.*
                            FROM shipments AS s;
            """.trimIndent())
            .print()
    }

}

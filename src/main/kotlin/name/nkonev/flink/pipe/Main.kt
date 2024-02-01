package name.nkonev.flink.pipe

import org.apache.commons.text.StringSubstitutor
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*


class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Main().runStream()
        }
    }

    val logger = LoggerFactory.getLogger(this.javaClass)

    fun runStream() {

        val config = readConfig("/config.properties")
        val configuration = Configuration.fromMap(config)

        val environment = StreamExecutionEnvironment
            .createLocalEnvironmentWithWebUI(configuration)

        environment.parallelism = 3

        // Checkpoint Configurations
        environment.enableCheckpointing(5000)
        environment.checkpointConfig.minPauseBetweenCheckpoints = 100

        val stateBackend = EmbeddedRocksDBStateBackend()
        stateBackend.setDbStoragePath(config.get("state.rocksdb.dir"))
        environment.stateBackend = stateBackend

        environment.checkpointConfig.externalizedCheckpointCleanup =
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION

        // Configure Restart Strategy
        environment.restartStrategy = RestartStrategies.fixedDelayRestart(5, Time.seconds(5))


        environment.use {

            val tableEnvironment = StreamTableEnvironment.create(it)

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
                                    'hostname' = '${config.get("postgres.host")}',
                                    'port' = '${config.get("postgres.port")}',
                                    'username' = '${config.get("postgres.username")}',
                                    'password' = '${config.get("postgres.password")}',
                                    'database-name' = '${config.get("postgres.database-name")}',
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

            logger.info("Apache Flink SQL tables:")
            tableEnvironment
                .executeSql("SHOW TABLES")
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


    private fun readConfig(path: String): Map<String, String> {
        val resource = this.javaClass.getResource(path)
        val appProps = Properties()
        val fis = resource.content as InputStream
        fis.use {
            appProps.load(it)
        }
        val propertiesMap = appProps
            .filter { it.key != null && it.value != null }
            .map { it.key.toString() to it.value.toString() }.toMap()

        val envPrefix = "flink__"
        val envMap = System.getenv().toMutableMap().filter {
            it.key.startsWith(envPrefix)
        }.map {
            it.key.removePrefix(envPrefix).replace('_', '.') to it.value
        }.toMap()


        val mergedMap = mutableMapOf<String, String>()

        mergedMap.putAll(propertiesMap)
        mergedMap.putAll(envMap)

        val resultMap = mergedMap.mapValues {
            StringSubstitutor.replaceSystemProperties(it.value)
        }

        return resultMap
    }

}

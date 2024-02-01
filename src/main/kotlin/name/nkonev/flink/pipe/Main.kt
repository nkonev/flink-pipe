package name.nkonev.flink.pipe

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import java.util.regex.Pattern


class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello there!")
            Main().runStream()
        }
    }

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
        val envPrefix = "flink__"

        val string = javaClass.getResource(path)?.readText(charset = Charsets.UTF_8) ?: ""
        val lines = string.split("\r?\n|\r".toRegex()).toTypedArray()
        val valuableLines = lines
            .filter { it.isNotEmpty() }
            .filter { it.get(0) != '#' }
            .filter { it.contains('=') }
        val propertiesMap =  valuableLines.map {
            val eqIdx = it.indexOf('=')

            val key = it.substring(0, eqIdx)
            val value = it.substring(eqIdx + 1)

            key to value
        }.toMap()


        val systemProperties = System.getenv().toMutableMap().filter {
            it.key.startsWith(envPrefix)
        }.map {
            it.key.removePrefix(envPrefix).replace('_', '.') to it.value
        }

        val resultMap = propertiesMap.toMutableMap()
        resultMap.putAll(systemProperties)

        val interpolationRegex = Pattern.compile(".*\\{(.*?)\\}.*").toRegex()
        // process placeholders as of system
        val changed = resultMap.map {
            val str = it.value
            if (str.matches(interpolationRegex)) {
                val found = interpolationRegex.find(str)

                val group = found?.groups?.get(1)
                val inBracesKey = group?.value
                val allRange = group?.range
                val firstPartEnd = allRange!!.start - 2
                val secondPartStart = allRange.endInclusive + 2
                val systemPropVal = System.getProperty(inBracesKey)
                val replacedString = str.substring(0, firstPartEnd) + systemPropVal + str.substring(secondPartStart)

                return@map it.key to replacedString
            } else {
                return@map it.key to it.value
            }
        }.toMap()

        return changed
    }

}

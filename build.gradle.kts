import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

group = "name.nkonev"
version = "0.1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.logback.classic)
    implementation(libs.flink.statebackend.rocksdb)
    implementation(libs.flink.json)
    implementation(libs.flink.clients)
    implementation(libs.flink.runtime.web)
    implementation(libs.flink.table.api.java.bridge)
    implementation(libs.flink.table.planner)

    implementation(libs.flink.sql.connector.postgres.cdc)

//    implementation("org.apache.flink:flink-connector-jdbc:3.1.1-1.17")
//    implementation("com.alibaba.ververica:ververica-connector-clickhouse:1.15-vvr-6.0.2-3")
//    implementation("ru.ivi.opensource:flink-clickhouse-sink:1.4.0")
    implementation("org.apache.flink:flink-connector-clickhouse:1.17.1-SNAPSHOT")
    implementation("org.apache.flink:flink-sql-connector-clickhouse:1.17.1-SNAPSHOT")

    implementation(libs.commons.lang)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<ShadowJar> {
    isZip64 = true // needed because lots of files
    mergeServiceFiles() // required for run flink correctly (https://habr.com/ru/companies/ru_mts/articles/775970/)
    manifest {
        attributes["Main-Class"] = "name.nkonev.flink.pipe.Main"
    }
}

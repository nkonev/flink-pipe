import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val flinkVersion: String by project
val logbackVersion: String by project
val ververicaConnectorVersion: String by project
val commonsLangVersion: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "name.nkonev"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.flink:flink-statebackend-rocksdb:$flinkVersion")
    implementation("org.apache.flink:flink-json:$flinkVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.apache.flink:flink-clients:$flinkVersion")
    implementation("org.apache.flink:flink-runtime-web:$flinkVersion")
    implementation("org.apache.flink:flink-table-api-java-bridge:$flinkVersion")
    implementation("org.apache.flink:flink-table-planner_2.12:$flinkVersion")
    implementation("com.ververica:flink-sql-connector-postgres-cdc:$ververicaConnectorVersion")

    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
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

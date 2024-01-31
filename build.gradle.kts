import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val flinkVersion: String by project
val logbackVersion: String by project
val ververicaConnectorVersion: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    application
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

    implementation("org.apache.commons:commons-configuration2:2.9.0")
    implementation("commons-beanutils:commons-beanutils:1.9.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    from(
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

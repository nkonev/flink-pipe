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
}

dependencies {
    implementation(libs.logback.classic)
    implementation(libs.flink.statebackend.rocksdb)
    implementation(libs.flink.json)
    implementation(libs.flink.clients)
    implementation(libs.flink.runtime.web)
    implementation(libs.flink.table.api.java.bridge)
    implementation(libs.flink.table.planner)

    implementation("org.apache.flink:flink-connector-base:1.18.1")
    implementation(libs.flink.sql.connector.postgres.cdc)

    implementation(libs.commons.lang)

    implementation(libs.flink.connector.jdbc)
    implementation(libs.postgresql.jdbc)

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

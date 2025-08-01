plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.0"
}

group = "ua.developer.artemmotuznyi"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    // HTML and form handling
    implementation("io.ktor:ktor-server-html-builder:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-call-logging:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.version.get()}")

    // Gmail API
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20220404-2.0.0")

    // Mail token module
    implementation(project(":common:mail-token"))

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(21)
}
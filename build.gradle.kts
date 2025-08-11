plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.0"
}

kotlin {
    jvmToolchain(17)
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
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    implementation("io.ktor:ktor-server-html-builder:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-call-logging:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.version.get()}")
    
    // Security enhancements
    implementation("io.ktor:ktor-server-rate-limit:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-cors:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-default-headers:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-hsts:${libs.versions.ktor.version.get()}")

    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")

    implementation("com.zaxxer:HikariCP:7.0.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20220404-2.0.0")

    // Security for password hashing
    implementation("org.springframework.security:spring-security-crypto:6.4.4")

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

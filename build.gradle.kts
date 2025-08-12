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

// Task to run the password hasher utility
tasks.register<JavaExec>("hashPassword") {
    group = "security"
    description = "Hash passwords for API credentials"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ua.developer.artemmotuznyi.tools.PasswordHasherKt"
    
    // Allow passing args
    doFirst {
        val userArgs = project.findProperty("args") as String?
        if (userArgs != null) {
            args = listOf("hash-password") + userArgs.split(" ")
        } else {
            args = listOf("hash-password")
        }
    }
}

// Task to run API security tests
tasks.register<JavaExec>("testApiSecurity") {
    group = "security"
    description = "Test API security components"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ua.developer.artemmotuznyi.test.ApiSecurityTestKt"
    args = listOf("test-api")
}

// Task to test real authentication
tasks.register<JavaExec>("testRealAuth") {
    group = "security"
    description = "Test real API authentication flow"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ua.developer.artemmotuznyi.test.RealAuthTestKt"
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
    implementation(project(":ukrsib-parser"))
    implementation(project(":common:database"))
    implementation(project(":common:mail-token"))
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
    
    // JWT Authentication for API
    implementation("io.ktor:ktor-server-auth:${libs.versions.ktor.version.get()}")
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.version.get()}")
    
    // BCrypt for API credentials (already used in AuthService)
    implementation("org.springframework.security:spring-security-crypto:5.7.2")
}

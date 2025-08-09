plugins {
    kotlin("jvm")
}

group = "ua.developer.artemmotuznyi"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    api("com.zaxxer:HikariCP:7.0.0")
    api("org.jetbrains.exposed:exposed-core:0.41.1")
    api("org.jetbrains.exposed:exposed-dao:0.41.1")
    api("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    api("org.postgresql:postgresql:42.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
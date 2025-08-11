package ua.developer.artemmotuznyi.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCORS() {
    install(CORS) {
        allowHost("localhost:8080") // Development
        allowHost(System.getenv("ALLOWED_HOST") ?: "localhost:8080") // Production
        allowCredentials = true
    }
}
package ua.developer.artemmotuznyi.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.hsts.HSTS

fun Application.configureSecurity() {
    install(HSTS) {
        includeSubDomains = true
        maxAgeInSeconds = 31536000 // 1 year
    }
}
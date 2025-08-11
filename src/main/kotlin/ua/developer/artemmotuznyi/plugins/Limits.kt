package ua.developer.artemmotuznyi.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimits() {
    install(RateLimit) {
        // Rate limiting for authentication endpoints
        register(RateLimitName("auth")) {
            rateLimiter(limit = 5, refillPeriod = 60.seconds)
            requestKey { call ->
                call.request.headers["X-Forwarded-For"]
                    ?: call.request.local.remoteAddress
            }
        }
    }
}
package ua.developer.artemmotuznyi

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.ratelimit.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ua.developer.artemmotuznyi.mailtoken.OAuthTokens
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    // Security configurations
    install(HSTS) {
        // Only enable HSTS in production
        includeSubDomains = true
        maxAgeInSeconds = 31536000 // 1 year
    }
    
    install(DefaultHeaders) {
        // Security headers
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
    }
    
    install(CORS) {
        // Configure CORS for production
        allowHost("localhost:8080") // Development
        allowHost(System.getenv("ALLOWED_HOST") ?: "localhost:8080") // Production
        allowCredentials = true
    }
    
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

    DatabaseFactory.init(
        jdbcUrl = System.getenv("DATABASE_URL")!!,
        user = System.getenv("DATABASE_USER")!!,
        password = System.getenv("DATABASE_PASSWORD")!!
    )
    transaction {
        exec("CREATE EXTENSION IF NOT EXISTS pgcrypto;")
        SchemaUtils.create(OAuthTokens)
    }

    install(ContentNegotiation){
        json()
    }

    configureRouting()
}

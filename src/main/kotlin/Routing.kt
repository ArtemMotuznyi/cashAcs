package ua.developer.artemmotuznyi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.ratelimit.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ul
import org.slf4j.LoggerFactory
import ua.developer.artemmotuznyi.api.*
import ua.developer.artemmotuznyi.responce.CashValue
import ua.developer.artemmotuznyi.responce.CashValueResponse
import ua.developer.artemmotuznyi.security.ApiCredentialsService
import ua.developer.artemmotuznyi.security.JwtService
import ua.developer.artemmotuznyi.ui.generateAuthFormHtml
import ua.developer.artemmotuznyi.ukrsibparser.AuthService
import ua.developer.artemmotuznyi.ukrsibparser.GmailService
import ua.developer.artemmotuznyi.ukrsibparser.UrksibCashInfoProvider
import java.text.SimpleDateFormat
import java.util.*

fun Application.configureRouting() {
    val log = LoggerFactory.getLogger("Routing")
    val authService = AuthService()
    val gmailService = GmailService()
    val urksibCashInfoProvider = UrksibCashInfoProvider(gmailService)
    
    // API services
    val jwtService = JwtService()
    val apiCredentialsService = ApiCredentialsService()

    routing {
        get("/") {
            log.info("GET /")
            call.respondText("Hello World!")
        }

        get("/auth") {
            log.info("GET /auth")
            val error = call.request.queryParameters["error"]
            call.respondHtml {
                generateAuthFormHtml(error)
            }
        }

        rateLimit(RateLimitName("auth")) {
            post("/auth") {
                log.info("POST /auth")
                val params = call.receiveParameters()
                val username = params["username"]?.trim()?.take(100) // Limit length and sanitize
                val password = params["password"]?.take(100) // Limit length
                
                // Input validation
                if (username.isNullOrBlank() || password.isNullOrBlank()) {
                    log.warn("Missing username or password")
                    call.respondRedirect("/auth?error=invalid")
                    return@post
                }
                
                // Sanitize username for logging (remove potential injection)
                val safeUsername = username.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
                log.debug("Received credentials username='{}'", safeUsername)

                if (!authService.validateCredentials(username, password)) {
                    log.warn("Invalid credentials for user='{}'", safeUsername)
                    call.respondRedirect("/auth?error=invalid")
                    return@post
                }

                val googleUrl = gmailService.getAuthorizationUrl()
                log.info("Redirecting to Google OAuth")
                call.respondRedirect(googleUrl)
            }
        }

        get("/oauth2callback") {
            log.info("GET /oauth2callback")
            val code = call.request.queryParameters["code"]?.trim()?.take(500) // Limit length
            if (code.isNullOrBlank()) {
                log.error("Missing 'code' parameter in callback")
                call.respond(HttpStatusCode.BadRequest, "Missing code")
                return@get
            }

            // Validate code format (basic security check)
            if (!code.matches(Regex("^[a-zA-Z0-9/_-]+$"))) {
                log.error("Invalid code format in callback")
                call.respond(HttpStatusCode.BadRequest, "Invalid code format")
                return@get
            }

            val success = gmailService.handleOAuthCallback(code)
            if (success) {
                log.info("OAuth callback handled successfully")
                call.respondText("✅ Authentication successful! Токени збережено.")
            } else {
                log.error("OAuth callback handling failed")
                call.respondRedirect("/auth?error=auth_failed")
            }
        }

        get("/emails") {
            log.info("GET /emails")
            if (!gmailService.hasValidToken()) {
                log.info("No valid token, redirecting to /auth")
                call.respondRedirect("/auth")
                return@get
            }

            try {
                val emails = gmailService.tryGetEmailsOrThrow()
                log.info("Rendering {} emails", emails.size)
                call.respondHtml {
                    body {
                        h1 { +"Your latest emails" }
                        ul {
                            emails.forEach { email ->
                                li { +email }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                log.error("Failed to fetch emails, redirecting to /auth", ex)
                call.respondRedirect("/auth")
            }
        }

        // API Routes - secured with JWT
        route("/api/v1") {
            
            // API Authentication endpoints
            rateLimit(RateLimitName("api-auth")) {
                post("/login") {
                    log.info("POST /api/v1/login")
                    
                    try {
                        val loginRequest = call.receive<LoginRequest>()
                        
                        // Input validation
                        if (loginRequest.username.isBlank() || loginRequest.password.isBlank()) {
                            log.warn("API login: missing username or password")
                            call.respond(HttpStatusCode.BadRequest, 
                                ApiErrorResponse("invalid_request", "Username and password are required"))
                            return@post
                        }
                        
                        // Sanitize username for logging
                        val safeUsername = loginRequest.username.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
                        log.debug("API login attempt for user: '{}'", safeUsername)
                        
                        if (!apiCredentialsService.validateCredentials(loginRequest.username, loginRequest.password)) {
                            log.warn("API login: invalid credentials for user '{}'", safeUsername)
                            call.respond(HttpStatusCode.Unauthorized,
                                ApiErrorResponse("invalid_credentials", "Invalid username or password"))
                            return@post
                        }
                        
                        // Generate JWT tokens
                        val accessToken = jwtService.generateToken(loginRequest.username, expirationHours = 24)
                        val refreshToken = jwtService.generateRefreshToken(loginRequest.username)
                        
                        log.info("API login successful for user '{}'", safeUsername)
                        call.respond(LoginResponse(
                            success = true,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresIn = 24 * 3600, // 24 hours in seconds
                            message = "Login successful"
                        ))
                        
                    } catch (e: Exception) {
                        log.error("API login error", e)
                        call.respond(HttpStatusCode.BadRequest,
                            ApiErrorResponse("invalid_request", "Invalid request format"))
                    }
                }
                
                post("/refresh") {
                    log.info("POST /api/v1/refresh")
                    
                    try {
                        val refreshRequest = call.receive<RefreshTokenRequest>()
                        
                        if (refreshRequest.refreshToken.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest,
                                ApiErrorResponse("invalid_request", "Refresh token is required"))
                            return@post
                        }
                        
                        // Validate refresh token
                        val tokenResult = jwtService.validateToken(refreshRequest.refreshToken)
                        if (tokenResult == null || tokenResult.tokenType != "refresh") {
                            log.warn("API refresh: invalid refresh token")
                            call.respond(HttpStatusCode.Unauthorized,
                                ApiErrorResponse("invalid_token", "Invalid or expired refresh token"))
                            return@post
                        }
                        
                        // Verify user still exists
                        if (!apiCredentialsService.isValidUsername(tokenResult.userId)) {
                            log.warn("API refresh: user no longer valid: '{}'", tokenResult.userId)
                            call.respond(HttpStatusCode.Unauthorized,
                                ApiErrorResponse("invalid_user", "User no longer valid"))
                            return@post
                        }
                        
                        // Generate new access token
                        val accessToken = jwtService.generateToken(tokenResult.userId, expirationHours = 24)
                        
                        log.info("API refresh successful for user '{}'", tokenResult.userId)
                        call.respond(RefreshTokenResponse(
                            success = true,
                            accessToken = accessToken,
                            expiresIn = 24 * 3600 // 24 hours in seconds
                        ))
                        
                    } catch (e: Exception) {
                        log.error("API refresh error", e)
                        call.respond(HttpStatusCode.BadRequest,
                            ApiErrorResponse("invalid_request", "Invalid request format"))
                    }
                }
            }
            
            // Protected API endpoints
            authenticate("api-jwt") {
                get("/status") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("user_id")?.asString()
                    val expiresAt = principal?.payload?.expiresAt
                    
                    log.info("API status check for user '{}'", userId)
                    
                    call.respond(ApiStatusResponse(
                        status = "authenticated",
                        user = userId,
                        tokenExpiresAt = expiresAt?.let { 
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply { 
                                timeZone = TimeZone.getTimeZone("UTC") 
                            }.format(it) 
                        }
                    ))
                }
                
                get("/cash") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("user_id")?.asString()
                    
                    log.info("API cash request from user '{}'", userId)
                    
                    if (!gmailService.hasValidToken()) {
                        log.warn("API cash: no valid Gmail token for data retrieval")
                        call.respond(HttpStatusCode.ServiceUnavailable,
                            ApiErrorResponse("service_unavailable", "Gmail service not available"))
                        return@get
                    }

                    try {
                        log.info("Building currency update...")
                        val urkSibValues = urksibCashInfoProvider.getCashInfo()
                        val cashValues = urkSibValues.map { (currency, value) ->
                            CashValue(
                                provider = "urksib",
                                currencyTitle = currency,
                                value = value
                            )
                        }

                        val cashValueResponse = CashValueResponse(cashValues = cashValues)

                        log.info("Returning currency update to user '{}': {}", userId, cashValueResponse)
                        call.respond(cashValueResponse)
                    } catch (ex: Exception) {
                        log.error("Failed to build currency update for user '{}'", userId, ex)
                        call.respond(HttpStatusCode.InternalServerError,
                            ApiErrorResponse("internal_error", "Unable to retrieve cash information"))
                    }
                }
            }
        }

        // Legacy cash endpoint (keep for backward compatibility but require Gmail OAuth)
        get("/cash") {
            log.info("GET /ukrsib-update called")

            if (!gmailService.hasValidToken()) {
                log.warn("Access denied: no valid Gmail token")
                call.respond(HttpStatusCode.Forbidden, "Access denied")
                return@get
            }

            try {
                log.info("Building currency update...")
                val urkSibValues = urksibCashInfoProvider.getCashInfo()
                val cashValues = urkSibValues.map { (currency, value) ->
                    CashValue(
                        provider = "urksib",
                        currencyTitle = currency,
                        value = value
                    )
                }

                val cashValueResponse = CashValueResponse(cashValues = cashValues)

                log.info("Returning currency update: {}", cashValueResponse)
                call.respond(cashValueResponse)
            } catch (ex: Exception) {
                log.error("Failed to build currency update", ex)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Unable to compute update"
                )
            }
        }
    }
}


package ua.developer.artemmotuznyi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ul
import org.slf4j.LoggerFactory
import ua.developer.artemmotuznyi.responce.CashValue
import ua.developer.artemmotuznyi.responce.CashValueResponse
import ua.developer.artemmotuznyi.ui.generateAuthFormHtml
import ua.developer.artemmotuznyi.ukrsib.GmailService
import ua.developer.artemmotuznyi.ukrsib.UrksibCashInfoProvider

fun Application.configureRouting() {
    val log = LoggerFactory.getLogger("Routing")
    val authService = AuthService()
    val gmailService = GmailService()
    val urksibCashInfoProvider = UrksibCashInfoProvider(gmailService)

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
                        provider = "ua/developer/artemmotuznyi/ukrsibeloper/artemmotuznyi/ukrsib",
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


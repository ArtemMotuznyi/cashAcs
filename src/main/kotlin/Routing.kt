package ua.developer.artemmotuznyi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
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
import ua.developer.artemmotuznyi.ukrsibparser.AuthService
import ua.developer.artemmotuznyi.ukrsibparser.GmailService
import ua.developer.artemmotuznyi.ukrsibparser.UrksibCashInfoProvider

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

        post("/auth") {
            log.info("POST /auth")
            val params = call.receiveParameters()
            val username = params["username"].orEmpty()
            val password = params["password"].orEmpty()
            log.debug("Received credentials username='{}'", username)

            if (!authService.validateCredentials(username, password)) {
                log.warn("Invalid credentials for user='{}'", username)
                call.respondRedirect("/auth?error=invalid")
                return@post
            }

            val googleUrl = gmailService.getAuthorizationUrl()
            log.info("Redirecting to Google OAuth: {}", googleUrl)
            call.respondRedirect(googleUrl)
        }

        get("/oauth2callback") {
            log.info("GET /oauth2callback")
            val code = call.request.queryParameters["code"]
            if (code.isNullOrBlank()) {
                log.error("Missing 'code' parameter in callback")
                call.respond(HttpStatusCode.BadRequest, "Missing code")
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


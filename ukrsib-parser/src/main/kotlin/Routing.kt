package ua.developer.artemmotuznyi.ukrsibparser

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import ua.developer.artemmotuznyi.ukrsibparser.ui.generateAuthFormHtml
import ua.developer.artemmotuznyi.ukrsibparser.ui.generateErrorHtml

fun Application.configureRouting() {
    val authService = AuthService()
    // TODO: Implement dependency injection
    // val tokenRepository = TokenRepository()
    // val gmailService = GmailService(tokenRepository)

    routing {
        get("/") {
            call.respondText("Welcome to the UKRSIB Parser!")
        }

        get("/auth") {
            call.respondHtml {
                generateAuthFormHtml {
                    call.request.queryParameters["error"]?.let { error ->
                        generateErrorHtml(error)
                    }
                }
            }
        }

        post("/auth") {
            val parameters = call.receiveParameters()
            val username = parameters["username"] ?: ""
            val password = parameters["password"] ?: ""

            if (authService.validateCredentials(username, password)) {
                // TODO: Implement proper session management
                call.respondText("Login successful!")
            } else {
                call.respondRedirect("/auth?error=invalid")
            }
        }
    }
}
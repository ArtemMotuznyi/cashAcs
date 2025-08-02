package ua.developer.artemmotuznyi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ua.developer.artemmotuznyi.ui.generateAuthFormHtml
import ua.developer.artemmotuznyi.ukrsibparser.AuthService
import ua.developer.artemmotuznyi.ukrsibparser.GmailService

fun Application.configureRouting() {
    val authService = AuthService()
    val gmailService = GmailService()

    routing {
        // Домашня
        get("/") {
            call.respondText("Hello World!")
        }

        // 1) Форма логіну користувача (username/password)
        get("/auth") {
            val error = call.request.queryParameters["error"]
            call.respondHtml {
                generateAuthFormHtml(error)
            }
        }

        // 2) Обробка уведення username/password
        post("/auth") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""

            if (!authService.validateCredentials(username, password)) {
                // невірні дані — редірект назад із помилкою
                call.respondRedirect("/auth?error=invalid")
                return@post
            }

            // Авторизація пройдена — редіректимо на Google
            val googleUrl = gmailService.getAuthorizationUrl()
            call.respondRedirect(googleUrl)
        }

        // 3) OAuth callback від Google
        get("/oauth2callback") {
            val code = call.request.queryParameters["code"]
            if (code.isNullOrBlank()) {
                call.respondText(
                    "Missing or empty `code` parameter",
                    status = HttpStatusCode.BadRequest
                )
                return@get
            }

            // Обробляємо callback — обмінюємо код на токени
            val success = gmailService.handleOAuthCallback(code)
            if (success) {
                // Можна редіректити далі в додаток
                call.respondText("✅ Gmail authenticated and tokens saved!")
            } else {
                call.respondText(
                    "Failed to authenticate with Gmail",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}

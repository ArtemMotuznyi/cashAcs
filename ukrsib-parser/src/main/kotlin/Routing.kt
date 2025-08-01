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
//    val tokenRepository = TokenRepository()
//    val gmailService = GmailService()

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
                call.respondText("Login successful!")
            } else {
                call.respondText("Login failed!", status = HttpStatusCode.Unauthorized)
            }
        }

        post("/auth"){
            val parameters = call.receiveParameters()
            val username = parameters["username"] ?: ""
            val password = parameters["password"] ?: ""

            if (authService.validateCredentials(username, password)) {
//                val authSuccess = gmailService.authenticate()

            } else {
                call.respondRedirect("/auth?error=invalid")
            }
        }

//        post("/login") {
//            val credentials = call.receive<Credentials>()
//            val authService = AuthService()
//            if (authService.authenticate(credentials)) {
//                call.respondText("Login successful!")
//            } else {
//                call.respondText("Login failed!", status = HttpStatusCode.Unauthorized)
//            }
//        }
    }
}
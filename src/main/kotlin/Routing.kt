package ua.developer.artemmotuznyi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import ua.developer.artemmotuznyi.auth.AuthService
import ua.developer.artemmotuznyi.gmail.GmailService
import ua.developer.artemmotuznyi.gmail.SimpleTokenRepository

fun Application.configureRouting() {
    val authService = AuthService()
    val tokenRepository = SimpleTokenRepository("secrets/master_key")
    val gmailService = GmailService(tokenRepository)
    
    routing {
        get("/") {
            call.respondText("Welcome to Cash ACS! Go to /auth to authenticate.")
        }
        
        get("/auth") {
            call.respondHtml {
                head {
                    title("Cash ACS - Authentication")
                    style {
                        +"""
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .container { max-width: 400px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
                        .form-group { margin-bottom: 15px; }
                        label { display: block; margin-bottom: 5px; font-weight: bold; }
                        input[type="text"], input[type="password"] { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
                        button { background-color: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
                        button:hover { background-color: #0056b3; }
                        .error { color: red; margin-top: 10px; }
                        """.trimIndent()
                    }
                }
                body {
                    div("container") {
                        h2 { +"Cash ACS Authentication" }
                        form(action = "/auth", method = FormMethod.post) {
                            div("form-group") {
                                label { +"Username:" }
                                input(type = InputType.text, name = "username") {
                                    required = true
                                }
                            }
                            div("form-group") {
                                label { +"Password:" }
                                input(type = InputType.password, name = "password") {
                                    required = true
                                }
                            }
                            button(type = ButtonType.submit) { +"Login" }
                        }
                        
                        call.request.queryParameters["error"]?.let { error ->
                            div("error") {
                                +when (error) {
                                    "invalid" -> "Invalid username or password"
                                    "auth_failed" -> "Gmail authentication failed"
                                    else -> "An error occurred"
                                }
                            }
                        }
                    }
                }
            }
        }
        
        post("/auth") {
            val parameters = call.receiveParameters()
            val username = parameters["username"] ?: ""
            val password = parameters["password"] ?: ""
            
            if (authService.validateCredentials(username, password)) {
                // Credentials are valid, start Gmail OAuth
                val authSuccess = gmailService.authenticate()
                if (authSuccess) {
                    call.respondRedirect("/emails")
                } else {
                    call.respondRedirect("/auth?error=auth_failed")
                }
            } else {
                call.respondRedirect("/auth?error=invalid")
            }
        }
        
        get("/emails") {
            val emails = gmailService.getEmails()
            
            call.respondHtml {
                head {
                    title("Cash ACS - Gmail Emails")
                    style {
                        +"""
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .container { max-width: 800px; margin: 0 auto; }
                        .email-item { padding: 10px; border-bottom: 1px solid #eee; }
                        .back-link { display: inline-block; margin-bottom: 20px; color: #007bff; text-decoration: none; }
                        .back-link:hover { text-decoration: underline; }
                        """.trimIndent()
                    }
                }
                body {
                    div("container") {
                        a(href = "/auth", classes = "back-link") { +"← Back to Authentication" }
                        h2 { +"Gmail Emails" }
                        
                        if (emails.isEmpty()) {
                            p { +"No emails found or Gmail authentication failed." }
                        } else {
                            div {
                                emails.forEachIndexed { index, subject ->
                                    div("email-item") {
                                        strong { +"${index + 1}. " }
                                        +subject
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

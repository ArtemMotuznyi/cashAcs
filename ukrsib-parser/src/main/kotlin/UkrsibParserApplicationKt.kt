package ua.developer.artemmotuznyi.ukrsibparser

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*


fun main() {
    // Порт можна задавати через ENV PORT (docker-compose)
    val port = System.getenv("PORT")?.toInt() ?: 8081

    embeddedServer(Netty, host = "0.0.0.0", port = port, module = Application::ukrsibParserModule)
        .start(wait = true)
}

/**
 * Основний Ktor-модуль для ukrsib-parser
 */
fun Application.ukrsibParserModule() {
    routing {
        route("/") {
            // Простий health‑check
            get("status") {
                call.respondText("ukrsib-parser is up!")


            }
            // Ендпоінт, який приймає name/email і повертає результат парсингу
            post("parse") {
                val payload = call.receive<ParseRequest>()
                val result = UkrsibParser.parseEmails(from = payload.sender)
                call.respond(result)
            }
        }
    }
}

/** DTO для запиту на парсинг */
data class ParseRequest(val sender: String)

/** Примірна реалізація парсера */
object UkrsibParser {
    fun parseEmails(from: String): List<ParsedEmail> {
        // тут твоя логіка: Gmail API → filter by "from" → парсинг
        return listOf() // повертай список ParsedEmail
    }
}

/** DTO для результату парсингу */
data class ParsedEmail(
    val id: String,
    val subject: String?,
    val date: String?,
    val snippet: String?
)

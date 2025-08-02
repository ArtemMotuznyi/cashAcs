package ua.developer.artemmotuznyi

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ua.developer.artemmotuznyi.mailtoken.OAuthTokens

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(
        jdbcUrl = System.getenv("DATABASE_URL")!!,
        user = System.getenv("DATABASE_USER")!!,
        password = System.getenv("DATABASE_PASSWORD")!!
    )
    transaction {
        exec("CREATE EXTENSION IF NOT EXISTS pgcrypto;")
        SchemaUtils.create(OAuthTokens)
    }

    configureRouting()
}

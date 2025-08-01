package ua.developer.artemmotuznyi.mailtoken

import org.jetbrains.exposed.sql.*


object OAuthTokens : Table("oauth_tokens") {
    val userId = varchar("user_id", 50)
    val tokenData = binary("token_data")

    override val primaryKey = PrimaryKey(userId)
}

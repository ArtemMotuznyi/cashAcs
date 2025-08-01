package ua.developer.artemmotuznyi.mailtoken

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.transactions.transaction
import ua.developer.artemmotuznyi.DatabaseFactory
import java.io.File
import java.sql.PreparedStatement

class TokenRepository(private val masterKeyPath: String) {
    private val masterKey: String by lazy {
        File(masterKeyPath).readText().trim()
    }

    suspend fun save(userId: String, dto: TokenDTO): Unit = withContext(Dispatchers.IO) {
        val json = Json.encodeToString(TokenDTO.serializer(), dto)
        val key = masterKey

        // Obtain a raw JDBC connection from Hikari
        DatabaseFactory.dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO oauth_tokens(user_id, token_data)
                VALUES (?, pgp_sym_encrypt(?, ?))
                ON CONFLICT (user_id) DO UPDATE
                  SET token_data = pgp_sym_encrypt(?, ?)
            """.trimIndent()
            ).use { ps ->
                ps.setString(1, userId)
                ps.setString(2, json)
                ps.setString(3, key)
                ps.setString(4, json)
                ps.setString(5, key)
                ps.executeUpdate()
            }
        }
    }

    suspend fun load(userId: String): TokenDTO? = withContext(Dispatchers.IO) {
        val key = masterKey
        val jsonString = DatabaseFactory.dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT pgp_sym_decrypt(token_data, ?)::text AS json FROM oauth_tokens WHERE user_id = ?"
            ).apply {
                setString(1, key)
                setString(2, userId)
            }.executeQuery().use { rs ->
                if (rs.next()) rs.getString("json") else null
            }
        } ?: return@withContext null

        Json.decodeFromString(TokenDTO.serializer(), jsonString)
    }
}
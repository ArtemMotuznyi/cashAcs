package ua.developer.artemmotuznyi.token

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ua.developer.artemmotuznyi.db.DatabaseFactory
import java.io.File

class TokenRepository(private val masterKeyPath: String) {
    private val masterKey: String by lazy {
        val keyFile = File(masterKeyPath)
        if (!keyFile.exists()) {
            throw IllegalStateException("Master key file not found at: $masterKeyPath")
        }
        val key = keyFile.readText().trim()
        if (key.length < 32) {
            throw IllegalStateException("Master key must be at least 32 characters long")
        }
        key
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
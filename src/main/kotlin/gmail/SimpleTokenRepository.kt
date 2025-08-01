package ua.developer.artemmotuznyi.gmail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ua.developer.artemmotuznyi.mailtoken.TokenDTO
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A simple file-based token repository for demo purposes
 * This avoids the need for a PostgreSQL database to demonstrate the functionality
 */
class SimpleTokenRepository(private val masterKeyPath: String) : TokenRepositoryInterface {
    private val tokens = ConcurrentHashMap<String, TokenDTO>()
    private val storageDir = File("tokens_storage").apply { mkdirs() }
    
    private val masterKey: String by lazy {
        val keyFile = File(masterKeyPath)
        if (keyFile.exists()) {
            keyFile.readText().trim()
        } else {
            "default_demo_key"
        }
    }

    override suspend fun save(userId: String, dto: TokenDTO): Unit = withContext(Dispatchers.IO) {
        try {
            val json = Json.encodeToString(TokenDTO.serializer(), dto)
            val tokenFile = File(storageDir, "$userId.token")
            
            // Simple XOR encryption for demo purposes
            val encrypted = json.toByteArray().map { (it.toInt() xor masterKey.hashCode()).toByte() }.toByteArray()
            tokenFile.writeBytes(encrypted)
            
            tokens[userId] = dto
            println("Token saved for user: $userId")
        } catch (e: Exception) {
            println("Error saving token for user $userId: ${e.message}")
        }
    }

    override suspend fun load(userId: String): TokenDTO? = withContext(Dispatchers.IO) {
        try {
            // First check in-memory cache
            tokens[userId]?.let { return@withContext it }
            
            // Then check file storage
            val tokenFile = File(storageDir, "$userId.token")
            if (!tokenFile.exists()) {
                return@withContext null
            }
            
            val encrypted = tokenFile.readBytes()
            val decrypted = encrypted.map { (it.toInt() xor masterKey.hashCode()).toByte() }.toByteArray()
            val json = String(decrypted)
            
            val token = Json.decodeFromString(TokenDTO.serializer(), json)
            tokens[userId] = token
            token
        } catch (e: Exception) {
            println("Error loading token for user $userId: ${e.message}")
            null
        }
    }
}
package ua.developer.artemmotuznyi.ukrsibparser

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.MessagePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStreamReader
import ua.developer.artemmotuznyi.mailtoken.TokenDTO
import ua.developer.artemmotuznyi.mailtoken.TokenRepository
import java.nio.charset.StandardCharsets
import java.util.Base64

class GmailService {
    private val logger = LoggerFactory.getLogger("GmailService")
    private val USER_ID = "gmail_user"
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val scopes = listOf(GmailScopes.GMAIL_READONLY)
    private val credentialsPath = System.getenv("CLIENT_SECRET")
        ?: throw IllegalStateException("CLIENT_SECRET not set")
    
    // Make redirect URI configurable for server deployment
    private val redirectUri = System.getenv("OAUTH_REDIRECT_URI") 
        ?: "http://localhost:8080/oauth2callback"

    private fun loadClientSecrets() = FileInputStream(credentialsPath).use { stream ->
        GoogleClientSecrets.load(jsonFactory, InputStreamReader(stream))
    }

    private val tokenRepo = TokenRepository(
        masterKeyPath = System.getenv("MAIL_TOKEN_MASTER_KEY")
            ?: throw IllegalStateException("MAIL_TOKEN_MASTER_KEY not set")
    )

    /** URL для редіректу до Google (offline не потрібен) */
    fun getAuthorizationUrl(): String {
        val clientSecrets = loadClientSecrets()
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(MemoryDataStoreFactory())
            .setAccessType("offline")      // залишається, щоб отримати токен єдинож згоди
            .build()

        val url = flow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
            // щоб refreshToken був отриманий при першій авторизації
            .setApprovalPrompt("force")
            .build()
        logger.debug("Generated OAuth URL: {}", url)
        return url
    }

    /** HTTPS callback: зберігаємо accessToken, ігноруємо refreshToken */
    suspend fun handleOAuthCallback(code: String): Boolean = withContext(Dispatchers.IO) {
        logger.debug("OAuth callback code='{}'", code.take(10) + "…")
        return@withContext try {
            val clientSecrets = loadClientSecrets()
            val flow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, scopes
            )
                .setDataStoreFactory(MemoryDataStoreFactory())
                .setAccessType("offline")
                .build()

            val tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute()

            val credential = flow.createAndStoreCredential(tokenResponse, USER_ID)
            logger.debug("Obtained accessToken (expires at {})", credential.expirationTimeMilliseconds)

            // Зберігаємо лише accessToken + expiration
            val dto = TokenDTO(
                token = credential.accessToken,
                refreshToken = "",  // ігноруємо
                expirationTimeMillis = credential.expirationTimeMilliseconds ?: 0L
            )
            tokenRepo.save(USER_ID, dto)
            logger.info("Saved access token for '{}'", USER_ID)
            true
        } catch (ex: Exception) {
            logger.error("handleOAuthCallback failed", ex)
            false
        }
    }

    /** Чи є дійсний (не прострочений) token */
    suspend fun hasValidToken(): Boolean = withContext(Dispatchers.IO) {
        val dto = tokenRepo.load(USER_ID)
        return@withContext dto?.let {
            val valid = (it.expirationTimeMillis ?: 0) > System.currentTimeMillis()
            logger.debug("Token valid={}", valid)
            valid
        } ?: false
    }

    /** Повертає список тем, або кидає помилку, якщо accessToken сплив */
    suspend fun tryGetEmailsOrThrow(maxResults: Long = 10): List<String> = withContext(Dispatchers.IO) {
        logger.debug("Fetching up to {} emails", maxResults)
        val dto = tokenRepo.load(USER_ID)
            ?: throw IllegalStateException("No token stored")

        // Створюємо credential лише з accessToken
        val credential = GoogleCredential().setAccessToken(dto.token)
        val service = Gmail.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("KtorGmailApp")
            .build()

        // Якщо токен протух — тут впаде PSQLException або GoogleJsonResponseException(401)
        val msgList = service.users().messages().list("me")
            .setMaxResults(maxResults)
            .setQ("from:online@ukrsibbank.com")
            .execute()
            .messages ?: emptyList()

        logger.debug("Got {} message IDs", msgList.size)
        return@withContext msgList.map { msg ->
            val full = service.users().messages()
                .get("me", msg.id)
                .setFormat("full")
                .execute()

            val subject = full.payload.headers
                .firstOrNull { it.name == "Subject" }
                ?.value ?: "(No Subject)"

            // Шукаємо текстову частину
            val bodyText = extractBody(full.payload) ?: "(No body)"
            logger.debug("Email subject: {}", subject)
            return@map "Subject: $subject\nBody: $bodyText"
        }
    }

    /** Рекурсивно шукає та декодує текст із MessagePart */
    private fun extractBody(part: MessagePart): String? {
        // Якщо є тіло в цьому part
        part.body?.data?.let { encoded ->
            val bytes = Base64.getUrlDecoder().decode(encoded)
            return String(bytes, StandardCharsets.UTF_8)
        }
        // Інакше рекурсивно перевіряємо вложені частини
        part.parts?.forEach { sub ->
            extractBody(sub)?.let { return it }
        }
        return null
    }

}

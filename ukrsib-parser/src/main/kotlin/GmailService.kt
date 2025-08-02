package ua.developer.artemmotuznyi.ukrsibparser

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.GmailScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStreamReader
import ua.developer.artemmotuznyi.mailtoken.TokenDTO
import ua.developer.artemmotuznyi.mailtoken.TokenRepository

class GmailService {
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val scopes = listOf(GmailScopes.GMAIL_READONLY)

    private val credentialsPath = System.getenv("CLIENT_SECRET")
        ?: "/app/secrets/credentials.json"

    private fun loadClientSecrets() = FileInputStream(credentialsPath).use { stream ->
        GoogleClientSecrets.load(jsonFactory, InputStreamReader(stream))
    }

    /** Повертає URL для редіректу до Google */
    fun getAuthorizationUrl(): String {
        val clientSecrets = loadClientSecrets()
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(MemoryDataStoreFactory())
            .setAccessType("offline")
            .build()

        return flow.newAuthorizationUrl()
            .setRedirectUri("http://localhost:8080/oauth2callback")
            .build()
    }

    /** Обмінює код на токени та зберігає їх у репозиторії */
    suspend fun handleOAuthCallback(code: String): Boolean = withContext(Dispatchers.IO) {
        val clientSecrets = loadClientSecrets()
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(MemoryDataStoreFactory())
            .setAccessType("offline")
            .build()

        val tokenResponse = flow.newTokenRequest(code)
            .setRedirectUri("http://localhost:8080/oauth2callback")
            .execute()

        val credential = flow.createAndStoreCredential(tokenResponse, "user")

        // Зберігаємо у репозиторій в IO-контексті
        val tokenRepository = TokenRepository(
            masterKeyPath = System.getenv("MAIL_TOKEN_MASTER_KEY") ?: "master_key.txt"
        )
        tokenRepository.save(
            "gmail_user",
            TokenDTO(
                token = credential.accessToken,
                refreshToken = credential.refreshToken ?: "",
                expirationTimeMillis = credential.expirationTimeMilliseconds ?: 0L
            )
        )
        true
    }
}

package ua.developer.artemmotuznyi.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

class JwtService {
    
    private val jwtSecret: String by lazy {
        val secretFile = File(System.getenv("JWT_SECRET_FILE") ?: "/run/secrets/jwt_secret")
        if (!secretFile.exists()) {
            throw IllegalStateException("JWT secret file not found at: ${secretFile.path}")
        }
        val secret = secretFile.readText().trim()
        if (secret.length < 32) {
            throw IllegalStateException("JWT secret must be at least 32 characters long")
        }
        secret
    }
    
    private val algorithm: Algorithm by lazy {
        Algorithm.HMAC256(jwtSecret)
    }
    
    private val verifier: JWTVerifier by lazy {
        JWT.require(algorithm)
            .withIssuer(ISSUER)
            .build()
    }
    
    /**
     * Generate a JWT token for API authentication
     * @param userId The user ID (api_user_1 or api_user_2)
     * @param expirationHours Token expiration time in hours (default: 24 hours)
     * @return JWT token string
     */
    fun generateToken(userId: String, expirationHours: Long = 24): String {
        val now = Date()
        val expiration = Date(now.time + expirationHours.hours.inWholeMilliseconds)
        
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(userId)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .withClaim("user_id", userId)
            .sign(algorithm)
    }
    
    /**
     * Generate a refresh token with longer expiration
     * @param userId The user ID
     * @return JWT refresh token string
     */
    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + 7.days.inWholeMilliseconds)
        
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(userId)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .withClaim("user_id", userId)
            .withClaim("token_type", "refresh")
            .sign(algorithm)
    }
    
    /**
     * Validate a JWT token and extract user information
     * @param token JWT token string
     * @return TokenValidationResult with user ID if valid, null if invalid
     */
    fun validateToken(token: String): TokenValidationResult? {
        return try {
            val decodedJWT = verifier.verify(token)
            val userId = decodedJWT.getClaim("user_id").asString()
            val tokenType = decodedJWT.getClaim("token_type").asString() ?: "access"
            
            if (userId.isNullOrBlank()) {
                null
            } else {
                TokenValidationResult(
                    userId = userId,
                    issuedAt = decodedJWT.issuedAt,
                    expiresAt = decodedJWT.expiresAt,
                    tokenType = tokenType
                )
            }
        } catch (e: JWTVerificationException) {
            null
        }
    }
    
    /**
     * Create a JWT verifier for Ktor authentication
     */
    fun createVerifier(): JWTVerifier {
        return verifier
    }
    
    /**
     * Check if a token is a refresh token
     */
    fun isRefreshToken(token: String): Boolean {
        return try {
            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("token_type").asString() == "refresh"
        } catch (e: JWTVerificationException) {
            false
        }
    }
    
    companion object {
        private const val ISSUER = "cashacs-api"
    }
}

data class TokenValidationResult(
    val userId: String,
    val issuedAt: Date,
    val expiresAt: Date,
    val tokenType: String
)
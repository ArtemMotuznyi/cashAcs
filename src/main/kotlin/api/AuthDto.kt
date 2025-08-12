package ua.developer.artemmotuznyi.api

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null, // seconds
    val tokenType: String = "Bearer",
    val message: String? = null
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val expiresIn: Long? = null, // seconds
    val message: String? = null
)

@Serializable
data class ApiErrorResponse(
    val error: String,
    val message: String,
    val code: Int? = null
)

@Serializable
data class ApiStatusResponse(
    val status: String,
    val user: String? = null,
    val tokenExpiresAt: String? = null
)
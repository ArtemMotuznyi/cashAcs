package ua.developer.artemmotuznyi.token

import kotlinx.serialization.Serializable

@Serializable
data class TokenDTO(
    val token: String,
    val refreshToken: String?,
    val expirationTimeMillis: Long?
)


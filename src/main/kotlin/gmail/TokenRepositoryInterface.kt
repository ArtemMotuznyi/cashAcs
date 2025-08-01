package ua.developer.artemmotuznyi.gmail

import ua.developer.artemmotuznyi.mailtoken.TokenDTO

interface TokenRepositoryInterface {
    suspend fun save(userId: String, dto: TokenDTO)
    suspend fun load(userId: String): TokenDTO?
}
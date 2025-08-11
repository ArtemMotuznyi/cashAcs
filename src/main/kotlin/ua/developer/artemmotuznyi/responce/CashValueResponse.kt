package ua.developer.artemmotuznyi.responce

import kotlinx.serialization.Serializable

@Serializable
data class CashValueResponse(
    val cashValues: List<CashValue>,
)

@Serializable
data class CashValue(
    val provider: String,
    val currencyTitle: String,
    val value: Double,
)
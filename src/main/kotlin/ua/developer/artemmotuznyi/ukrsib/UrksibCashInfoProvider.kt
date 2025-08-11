package ua.developer.artemmotuznyi.ukrsib


class UrksibCashInfoProvider(
    private val gmailService: GmailService
) {

    suspend fun getCashInfo(): Map<String, Double> {
        val emails = gmailService.tryGetEmailsOrThrow(20) // fetch up to 50 emails
        val cashInfo = mutableMapOf<String, Double>()
        var uahIndex = -1
        var usdIndex = -1

        // Step 1: Find initial balances
        for ((i, email) in emails.withIndex()) {
            if (uahIndex == -1 && email.contains("валюта UAH, доступні кошти")) {
                val uahMatch = Regex("кошти\\s+([\\d.,]+)").find(email)
                val uahValue = uahMatch?.groupValues?.get(1)?.dropLast(1)?.toDoubleOrNull() ?: 0.0
                cashInfo["UAH"] = uahValue
                uahIndex = i
            }
            if (usdIndex == -1 && email.contains("валюта USD, доступні кошти")) {
                val usdMatch = Regex("кошти\\s+([\\d.,]+)").find(email)
                val usdValue = usdMatch?.groupValues?.get(1)?.dropLast(1)?.toDoubleOrNull() ?: 0.0
                cashInfo["USD"] = usdValue
                usdIndex = i
            }
            if (usdIndex != -1 && uahIndex != -1) {
                break
            }
        }



        return cashInfo.mapValues { entry ->
            entry.value + getChangeValue(
                emails.subList(0, if (entry.key == "UAH") uahIndex else usdIndex),
                entry.key
            )
        }
    }

    fun getChangeValue(emails: List<String>, currency: String): Double {
        var changes = 0.0

        for (email in emails) {
            if (!email.contains("рух коштів") || !email.contains(currency)) continue

            val opMatch = Regex("тип операції:? (зараховано|списано)").find(email)
            val sumMatch = Regex("сумма:\\s+([\\d.,]+)").find(email)
            val operation = opMatch?.groupValues?.get(1)
            val sum = sumMatch?.groupValues?.get(1)?.dropLast(1)?.toDoubleOrNull()
            if (operation != null && sum != null) {
                changes = if (operation == "зараховано") changes + sum else changes - sum
            }
        }

        return changes
    }

}
package com.prashant.droidkit.core.network

data class MockResponse(
    val id: String,
    val urlPattern: String,
    val method: String,
    val matchType: MatchType = MatchType.EXACT,
    val enabled: Boolean = true,
    val statusCode: Int,
    val responseBody: String,
    val headers: Map<String, String> = emptyMap(),
    val delayMs: Long = 0
) {
    enum class MatchType {
        EXACT,
        WILDCARD
    }

    fun matches(url: String, method: String): Boolean {
        if (!enabled) return false
        if (this.method != method) return false

        return when (matchType) {
            MatchType.EXACT -> url == urlPattern
            MatchType.WILDCARD -> {
                val pattern = urlPattern.replace("*", ".*")
                url.matches(Regex(pattern))
            }
        }
    }
}

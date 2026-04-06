package com.prashant.droidkit.core.network

data class NetworkCall(
    val id: String,
    val timestamp: Long,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseStatus: Int?,
    val responseMessage: String?,
    val responseHeaders: Map<String, String>?,
    val responseBody: String?,
    val duration: Long?,
    val error: String?,
    val isMocked: Boolean = false
)

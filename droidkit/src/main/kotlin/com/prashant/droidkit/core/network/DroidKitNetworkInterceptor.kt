package com.prashant.droidkit.core.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.net.URL
import java.util.UUID

internal class DroidKitNetworkInterceptor(context: Context) : Interceptor {
    private val mockRepository = MockRepository(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val url = request.url.toString()
        val method = request.method
        val host = request.url.host
        val path = request.url.encodedPath

        val requestHeaders = request.headers.toMap()
        val requestBody = try {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8().take(100_000)
            }
        } catch (e: Exception) {
            null
        }

        val mock = mockRepository.findMatch(url, method)
        if (mock != null) {
            Thread.sleep(mock.delayMs)

            val mockResponse = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(mock.statusCode)
                .message("OK")
                .body(mock.responseBody.toResponseBody("application/json".toMediaTypeOrNull()))
                .apply {
                    mock.headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()

            val duration = System.currentTimeMillis() - startTime
            val networkCall = NetworkCall(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                method = method,
                url = url,
                host = host,
                path = path,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseStatus = mock.statusCode,
                responseMessage = "Mocked",
                responseHeaders = mock.headers,
                responseBody = mock.responseBody.take(100_000),
                duration = duration,
                error = null,
                isMocked = true
            )

            NetworkCallRepository.add(networkCall)
            return mockResponse
        }

        var response: Response? = null
        var error: String? = null

        try {
            response = chain.proceed(request)

            val responseHeaders = response.headers.toMap()
            val responseBodyString = try {
                val source = response.body?.source()
                source?.request(Long.MAX_VALUE)
                val buffer = source?.buffer
                buffer?.clone()?.readUtf8()?.take(100_000)
            } catch (e: Exception) {
                null
            }

            val duration = System.currentTimeMillis() - startTime
            val networkCall = NetworkCall(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                method = method,
                url = url,
                host = host,
                path = path,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseStatus = response.code,
                responseMessage = response.message,
                responseHeaders = responseHeaders,
                responseBody = responseBodyString,
                duration = duration,
                error = null,
                isMocked = false
            )

            NetworkCallRepository.add(networkCall)
            return response

        } catch (e: IOException) {
            error = e.message ?: "Network error"

            val duration = System.currentTimeMillis() - startTime
            val networkCall = NetworkCall(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                method = method,
                url = url,
                host = host,
                path = path,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseStatus = null,
                responseMessage = null,
                responseHeaders = null,
                responseBody = null,
                duration = duration,
                error = error,
                isMocked = false
            )

            NetworkCallRepository.add(networkCall)
            throw e
        }
    }

    private fun okhttp3.Headers.toMap(): Map<String, String> {
        return names().associateWith { get(it) ?: "" }
    }
}

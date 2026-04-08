package com.vialink.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/// HTTP 클라이언트 (OkHttp + 지수 백오프 재시도)
internal class NetworkClient(
    private val baseURL: String,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", apiKey)
                .addHeader("User-Agent", "ViaLinkSDK/1.0.12 Android")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /// GET 요청
    suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        executeWithRetry {
            val request = Request.Builder()
                .url("$baseURL$path")
                .get()
                .build()
            client.newCall(request).execute()
        }
    }

    /// POST 요청
    suspend fun post(path: String, body: Map<String, Any?>): String = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject(body).toString()
            .toRequestBody(jsonMediaType)

        executeWithRetry {
            val request = Request.Builder()
                .url("$baseURL$path")
                .post(jsonBody)
                .build()
            client.newCall(request).execute()
        }
    }

    /// 링크 생성 API
    suspend fun createLink(
        path: String,
        data: Map<String, Any>?,
        campaign: String?
    ): Result<String> {
        return try {
            val body = mutableMapOf<String, Any?>(
                "deeplinkPath" to path
            )
            if (data != null) body["deeplinkData"] = data
            if (campaign != null) body["campaign"] = campaign

            val response = post("/api/links", body)
            val json = JSONObject(response)
            Result.success(json.getString("shortUrl"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /// 지수 백오프 재시도 (최대 3회)
    private suspend fun executeWithRetry(
        maxRetries: Int = 3,
        block: () -> okhttp3.Response
    ): String {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val response = block()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        return resp.body?.string() ?: ""
                    }
                    throw IOException("HTTP ${resp.code}")
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delayMs = (1L shl attempt) * 1000 // 1초, 2초, 4초
                    delay(delayMs)
                }
            }
        }

        throw lastException ?: IOException("요청 실패")
    }
}

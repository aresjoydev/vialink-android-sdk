package com.vialink.sdk

import android.net.Uri
import com.vialink.sdk.model.DeepLinkData
import org.json.JSONObject

/// App Link URL 파싱 + 서버 조회
internal class DeepLinkHandler {

    /// App Link URI에서 short_code 추출
    /// URI 형식: https://vialink.app/{slug}/{shortCode} — 마지막 세그먼트가 short code
    fun parseAppLink(uri: Uri): String? {
        val segments = uri.pathSegments
        if (segments.size >= 2) {
            return segments.last()
        }
        return null
    }

    /// 서버에서 딥링크 데이터 조회
    /// POST /v1/resolve — App Links로 앱이 직접 열렸을 때 short code로 딥링크 데이터 조회
    suspend fun fetchLinkData(shortCode: String, client: NetworkClient): DeepLinkData? {
        return try {
            val body = mapOf<String, Any?>("short_code" to shortCode)
            val response = client.post("/v1/resolve", body)
            val json = JSONObject(response)

            if (!json.optBoolean("matched", false)) return null

            val params = mutableMapOf<String, String>()
            val dataObj = json.optJSONObject("deeplink_data")
            if (dataObj != null) {
                for (key in dataObj.keys()) {
                    params[key] = dataObj.getString(key)
                }
            }

            DeepLinkData(
                path = json.optString("deeplink_path", "/"),
                params = params,
                shortCode = shortCode
            )
        } catch (e: Exception) {
            ViaLinkLog.error("링크 데이터 조회 실패", e)
            null
        }
    }

    companion object {
        /// 정적 메서드: URI에서 short_code 파싱
        fun parseShortCode(uri: Uri): String? {
            val segments = uri.pathSegments
            if (segments.size >= 2) {
                return segments.last()
            }
            return null
        }
    }
}

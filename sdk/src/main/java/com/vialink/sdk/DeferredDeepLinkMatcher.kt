package com.vialink.sdk

import com.vialink.sdk.model.DeepLinkData
import com.vialink.sdk.model.DeviceInfo
import org.json.JSONObject

/// 디퍼드 딥링크 매칭 (POST /v1/open)
/// 앱 첫 실행 시 핑거프린트 기반으로 서버에서 매칭 시도
internal class DeferredDeepLinkMatcher(
    private val client: NetworkClient,
    private val deviceInfo: DeviceInfo
) {

    /// 매칭 시도 - 성공 시 DeepLinkData 반환, 실패 시 null
    suspend fun match(fp: String? = null): DeepLinkData? {
        return try {
            val deviceMap = deviceInfo.toDictionary()
            ViaLinkLog.info("[디퍼드] device_info=$deviceMap fp=${fp?.take(16) ?: "없음"}")

            val body = mutableMapOf<String, Any?>(
                "device_info" to deviceMap,
                "is_first_launch" to true
            )
            if (fp != null) body["fp"] = fp

            ViaLinkLog.info("[디퍼드] POST /v1/open 요청 시작")
            val response = client.post("/v1/open", body)
            ViaLinkLog.info("[디퍼드] 응답: ${response.take(200)}")
            val json = JSONObject(response)

            if (!json.optBoolean("matched", false)) {
                return null
            }

            val path = json.optString("deeplink_path", "")
            if (path.isEmpty()) return null

            val params = mutableMapOf<String, String>()
            val rawParams = json.optJSONObject("deeplink_data")
            if (rawParams != null) {
                for (key in rawParams.keys()) {
                    params[key] = rawParams.get(key).toString()
                }
            }

            DeepLinkData(
                path = path,
                params = params,
                shortCode = json.optString("link_click_id", "").ifEmpty { null }
            )
        } catch (e: Exception) {
            ViaLinkLog.error("디퍼드 매칭 실패", e)
            null
        }
    }
}

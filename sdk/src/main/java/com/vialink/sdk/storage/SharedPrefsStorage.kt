package com.vialink.sdk.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import com.vialink.sdk.model.EventPayload

/// SharedPreferences 기반 영속 저장소
internal class SharedPrefsStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vialink_sdk", Context.MODE_PRIVATE)

    /// 첫 실행 여부
    var hasLaunched: Boolean
        get() = prefs.getBoolean(KEY_HAS_LAUNCHED, false)
        set(value) { prefs.edit().putBoolean(KEY_HAS_LAUNCHED, value).apply() }

    /// 미전송 이벤트 저장
    fun savePendingEvents(events: List<EventPayload>) {
        val jsonArray = JSONArray()
        for (event in events) {
            val obj = JSONObject().apply {
                put("link_id", event.linkId ?: 0)
                put("event_name", event.eventName)
                put("timestamp", event.timestamp)
                if (event.eventData != null) {
                    put("event_data", JSONObject(event.eventData))
                }
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PENDING_EVENTS, jsonArray.toString()).apply()
    }

    /// 미전송 이벤트 복원
    fun loadPendingEvents(): List<EventPayload> {
        val json = prefs.getString(KEY_PENDING_EVENTS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                val eventData = if (obj.has("event_data")) {
                    val dataObj = obj.getJSONObject("event_data")
                    dataObj.keys().asSequence().associateWith { key -> dataObj.get(key) }
                } else null

                EventPayload(
                    linkId = obj.optInt("link_id", 0).takeIf { it != 0 },
                    eventName = obj.getString("event_name"),
                    eventData = eventData,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /// 미전송 이벤트 삭제
    fun clearPendingEvents() {
        prefs.edit().remove(KEY_PENDING_EVENTS).apply()
    }

    /// 디퍼드 딥링킹용 fp 파라미터 저장
    fun saveFp(fp: String) {
        prefs.edit().putString(KEY_PENDING_FP, fp).apply()
    }

    /// 저장된 fp 파라미터 로드 후 삭제
    fun loadFp(): String? {
        val fp = prefs.getString(KEY_PENDING_FP, null)
        if (fp != null) prefs.edit().remove(KEY_PENDING_FP).apply()
        return fp
    }

    companion object {
        private const val KEY_HAS_LAUNCHED = "has_launched"
        private const val KEY_PENDING_EVENTS = "pending_events"
        private const val KEY_PENDING_FP = "pending_fp"
    }
}

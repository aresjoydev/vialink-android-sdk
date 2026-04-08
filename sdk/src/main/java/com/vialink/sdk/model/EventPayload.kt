package com.vialink.sdk.model

/// 이벤트 페이로드 모델
data class EventPayload(
    val linkId: Int? = null,
    val eventName: String,
    val eventData: Map<String, Any>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /// 단일 이벤트 전송용 딕셔너리
    fun toDictionary(): Map<String, Any?> = mapOf(
        "link_id" to (linkId ?: 0),
        "event_name" to eventName,
        "event_data" to (eventData ?: emptyMap<String, Any>())
    )

    /// 배치 전송용 딕셔너리
    fun toBatchItem(): Map<String, Any?> = mapOf(
        "link_id" to (linkId ?: 0),
        "event_name" to eventName
    )
}

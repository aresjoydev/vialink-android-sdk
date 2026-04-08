package com.vialink.sdk

import com.vialink.sdk.model.EventPayload
import org.junit.Assert.*
import org.junit.Test

class EventPayloadTest {

    @Test
    fun `이벤트 딕셔너리 변환`() {
        val event = EventPayload(
            linkId = 1,
            eventName = "purchase",
            eventData = mapOf("revenue" to "29900")
        )
        val dict = event.toDictionary()
        assertEquals("purchase", dict["event_name"])
        assertEquals(1, dict["link_id"])
    }

    @Test
    fun `linkId null일 때 기본값 0`() {
        val event = EventPayload(eventName = "view")
        val dict = event.toDictionary()
        assertEquals(0, dict["link_id"])
        assertEquals("view", dict["event_name"])
    }

    @Test
    fun `배치 아이템 변환`() {
        val event = EventPayload(eventName = "view")
        val batch = event.toBatchItem()
        assertEquals("view", batch["event_name"])
        assertEquals(0, batch["link_id"])
    }

    @Test
    fun `eventData null일 때 빈 맵`() {
        val event = EventPayload(eventName = "test")
        val dict = event.toDictionary()
        assertEquals(emptyMap<String, Any>(), dict["event_data"])
    }
}

package com.vialink.sdk

import com.vialink.sdk.model.DeepLinkData
import org.junit.Assert.*
import org.junit.Test

class DeepLinkDataTest {

    @Test
    fun `DeepLinkData 생성`() {
        val data = DeepLinkData(
            path = "/product/123",
            params = mapOf("id" to "123", "promo" to "SUMMER"),
            shortCode = "aB3xK"
        )
        assertEquals("/product/123", data.path)
        assertEquals("aB3xK", data.shortCode)
        assertEquals("123", data.params["id"])
        assertEquals("SUMMER", data.params["promo"])
    }

    @Test
    fun `기본값 확인`() {
        val data = DeepLinkData(path = "/home")
        assertTrue(data.params.isEmpty())
        assertNull(data.shortCode)
    }

    @Test
    fun `data class equals 동작`() {
        val data1 = DeepLinkData(path = "/test", shortCode = "abc")
        val data2 = DeepLinkData(path = "/test", shortCode = "abc")
        assertEquals(data1, data2)
    }

    @Test
    fun `data class copy 동작`() {
        val original = DeepLinkData(path = "/original", shortCode = "xyz")
        val copied = original.copy(path = "/copied")
        assertEquals("/copied", copied.path)
        assertEquals("xyz", copied.shortCode)
    }
}

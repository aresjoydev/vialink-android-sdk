package com.vialink.sdk

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeepLinkHandlerTest {

    @Test
    fun `slug+code URL에서 shortCode 파싱`() {
        val uri = Uri.parse("https://vialink.app/test/xYz12")
        val code = DeepLinkHandler.parseShortCode(uri)
        assertEquals("xYz12", code)
    }

    @Test
    fun `세그먼트 1개인 URL은 null`() {
        val uri = Uri.parse("https://vialink.app/only-one")
        val code = DeepLinkHandler.parseShortCode(uri)
        assertNull(code)
    }

    @Test
    fun `루트 URL은 null`() {
        val uri = Uri.parse("https://vialink.app/")
        val code = DeepLinkHandler.parseShortCode(uri)
        assertNull(code)
    }

    @Test
    fun `slug만 있고 code 없음`() {
        val uri = Uri.parse("https://vialink.app/test/")
        val code = DeepLinkHandler.parseShortCode(uri)
        // pathSegments에서 빈 문자열은 제외되므로 size가 1
        assertNull(code)
    }

    @Test
    fun `인스턴스 메서드 parseAppLink 동작`() {
        val handler = DeepLinkHandler()
        val uri = Uri.parse("https://vialink.app/myapp/aB3xK")
        val code = handler.parseAppLink(uri)
        assertEquals("aB3xK", code)
    }

    @Test
    fun `쿼리 파라미터 있는 URL`() {
        val uri = Uri.parse("https://vialink.app/test/abc123?ref=share")
        val code = DeepLinkHandler.parseShortCode(uri)
        assertEquals("abc123", code)
    }
}

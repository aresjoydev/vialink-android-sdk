package com.vialink.sample

import android.app.Application
import android.util.Log
import com.vialink.sdk.ViaLinkSDK

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. SDK 초기화
        ViaLinkSDK.init(this, "0b8edff0b2979ce9efd925f43208b6debfae9db87c970367ba594c76238b16a9")

        // 2. 딥링크 콜백 등록 (App Link로 앱이 열렸을 때)
        ViaLinkSDK.onDeepLink { data ->
            Log.d("ViaLink", "딥링크 수신: ${data.path}")
            DeepLinkState.postResult(
                "딥링크 수신 (App Link)",
                "경로: ${data.path}\n파라미터: ${data.params}\nshortCode: ${data.shortCode ?: "없음"}\nlinkId: ${data.linkId ?: "없음"}"
            )
        }

        // 3. 디퍼드 딥링크 콜백 등록 (첫 실행 시 매칭 결과)
        ViaLinkSDK.onDeferredDeepLink { data, error ->
            when {
                error != null -> {
                    Log.w("ViaLink", "디퍼드 매칭 실패: ${error.message}")
                    DeepLinkState.postResult(
                        "디퍼드 매칭 실패",
                        "오류: ${error.message}"
                    )
                }
                data != null -> {
                    Log.d("ViaLink", "디퍼드 딥링크 수신: ${data.path}")
                    DeepLinkState.postResult(
                        "디퍼드 딥링크 수신",
                        "경로: ${data.path}\n파라미터: ${data.params}\nshortCode: ${data.shortCode ?: "없음"}\nlinkId: ${data.linkId ?: "없음"}"
                    )
                }
            }
        }
    }
}

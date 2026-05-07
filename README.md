# ViaLink Android SDK

ViaLink 딥링크 인프라 서비스를 위한 Android SDK입니다.

## 요구사항

- Android API 24 (7.0)+
- Kotlin 1.9+

## 설치

### Gradle

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("com.vialink:sdk:<version>")
}
```

## 사용법

```kotlin
// 초기화 (Application.onCreate)
ViaLinkSDK.init(this, "YOUR_API_KEY")

// 딥링크 콜백
ViaLinkSDK.onDeepLink { data ->
    Log.d("ViaLink", "경로: ${data.path}")
    Log.d("ViaLink", "파라미터: ${data.params}")
}

// 디퍼드 딥링크 콜백
ViaLinkSDK.onDeferredDeepLink { data ->
    Log.d("ViaLink", "디퍼드: ${data.path}")
}

// Activity에서 Intent 처리
ViaLinkSDK.handleIntent(intent)

// 이벤트 추적
ViaLinkSDK.track("purchase", mapOf(
    "product_id" to "12345",
    "revenue" to 29900
))

// 링크 생성
val result = ViaLinkSDK.createLink(
    path = "/product/12345",
    data = mapOf("promo_code" to "FRIEND_SHARE"),
    campaign = "referral"
)
```

## 문서

- [SDK 가이드](https://docs.vialink.app)

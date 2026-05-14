# ViaLink Android SDK

ViaLink 딥링크 인프라 서비스를 위한 Android SDK입니다.

## 특징

- **딥링크 라우팅** — App Links / Custom Scheme 자동 처리
- **디퍼드 딥링킹** — 앱 설치 후 첫 실행 시 핑거프린트 기반 매칭
- **이벤트 추적** — 커스텀 이벤트 배치 전송
- **결제 어트리뷰션** — 결제 시도 기록 + 자동 link_id 첨부
- **링크 생성** — 앱 내에서 딥링크 생성 (static/dynamic)

## 요구사항

- Android API 24 (7.0)+
- Kotlin 1.9+

## 설치

### 1) 저장소 등록 (settings.gradle.kts)

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://aresjoydev.github.io/vialink-android-sdk") }
    }
}
```

### 2) 의존성 추가 (app/build.gradle.kts)

```kotlin
dependencies {
    implementation("com.vialink:sdk:<version>")
}
```

> 최신 버전은 [GitHub 저장소](https://github.com/aresjoydev/vialink-android-sdk)의 release 태그 또는 `https://aresjoydev.github.io/vialink-android-sdk/com/vialink/sdk/maven-metadata.xml` 에서 확인할 수 있습니다.

## 사용법

### 1. 초기화

```kotlin
// Application.onCreate 에서 초기화
ViaLinkSDK.init(this, "YOUR_API_KEY")
```

### 2. 딥링크 콜백

```kotlin
// App Link / 커스텀 스킴 수신
ViaLinkSDK.onDeepLink { data ->
    Log.d("ViaLink", "경로: ${data.path}")
    Log.d("ViaLink", "파라미터: ${data.params}")
}

// 디퍼드 딥링크 (첫 설치 후 매칭)
ViaLinkSDK.onDeferredDeepLink { data, error ->
    if (error != null) {
        Log.e("ViaLink", "매칭 실패: ${error.message}")
        return@onDeferredDeepLink
    }
    if (data != null) {
        Log.d("ViaLink", "디퍼드: ${data.path}")
    } else {
        Log.d("ViaLink", "매칭 결과 없음 (Organic)")
    }
}
```

**중요**: Intent 처리를 위해 Activity 에서 `handleIntent` 를 호출해야 합니다.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViaLinkSDK.handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ViaLinkSDK.handleIntent(intent)
    }
}
```

### 3. Pull API

```kotlin
// 동기 (캐시된 값 즉시 반환)
val deepLink = ViaLinkSDK.getDeepLinkData()
val deferred = ViaLinkSDK.getDeferredLinkData()

// 비동기 (결과 도착까지 대기, 코루틴 환경)
lifecycleScope.launch {
    val deepLinkAsync = ViaLinkSDK.awaitDeepLinkData()    // 3초 타임아웃
    val deferredAsync = ViaLinkSDK.awaitDeferredLinkData() // 결과까지 대기
}
```

### 4. 이벤트 추적

```kotlin
ViaLinkSDK.track("purchase", mapOf(
    "product_id" to "12345",
    "revenue" to 29900,
    "currency" to "KRW"
))
```

### 5. 결제 추적

```kotlin
lifecycleScope.launch {
    val result = ViaLinkSDK.trackPayment(
        PaymentInitiatedArgs(
            orderId = "ORD-2026-0001",
            amount = 19900.0,
            currency = "KRW",
            paymentMethod = "card"
        )
    )
    Log.d("ViaLink", "success: ${result.success}, id: ${result.paymentEventId}")
}
```

### 6. 링크 생성

```kotlin
lifecycleScope.launch {
    val result = ViaLinkSDK.createLink(
        path = "/product/12345",
        data = mapOf("promo_code" to "FRIEND_SHARE"),
        campaign = "referral",
        linkType = "dynamic" // 클릭 추적 필요 시
    )
    result.onSuccess { url -> Log.d("ViaLink", "생성된 링크: $url") }
    result.onFailure { err -> Log.e("ViaLink", "생성 실패: ${err.message}") }
}
```

## 샘플 프로젝트

`sample/` 디렉토리에서 실행 가능한 샘플 앱을 확인하세요.

## 문서

- [SDK 가이드](https://docs.vialink.app/sdk/android)

## 라이선스

MIT License — Aresjoy Inc.

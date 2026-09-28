# ViaLink Android SDK

[![ViaLink — 6개 플랫폼 딥링크를 무료로 시작하세요](docs/banner-ko.png)](https://vialink.app/?lang=ko&utm_source=github&utm_medium=readme&utm_campaign=android-sdk)

[English](README.md) | **한국어**

ViaLink 딥링크 인프라 서비스를 위한 Android SDK입니다.

링크 하나로 iOS · Android · Web을 자동 분기합니다. 앱이 설치돼 있지 않으면 스토어로
보낸 뒤, 설치 후 첫 실행에서 원래 의도한 화면으로 정확히 연결합니다(디퍼드 딥링킹).
클릭 → 설치 → 실행 → 이벤트 → 결제까지 하나의 파이프라인에서 어트리뷰션으로 이어집니다.

많은 딥링크 · 어트리뷰션 도구가 영업 문의와 연간 계약을 요구하는 것과 달리
**ViaLink는 무료로 시작합니다.** 카드 등록 없이, 가입 즉시 6개 플랫폼 SDK를 모두 쓸 수 있습니다.

**→ [vialink.app](https://vialink.app/?lang=ko&utm_source=github&utm_medium=readme&utm_campaign=android-sdk)**

## 인앱 브라우저에서도 앱이 열립니다

카카오톡·네이버·LINE·인스타그램·Threads에 공유된 링크는 각 앱의 내장 브라우저에서 열리고,
이 환경에서는 Universal Link / App Link가 동작하지 않는 경우가 많습니다. ViaLink 링크 서버는
인앱 브라우저를 감지해 그 환경에서 동작하는 경로를 고릅니다.

- **카카오톡 · 네이버 · LINE (iOS)** — Safari로 자동 전환해 Universal Link가 동작하게 합니다
- **Android 인앱 브라우저** — `intent://` URL로 앱을 실행하고, 미설치 시 스토어로 보냅니다
- **인스타그램 · 페이스북 · Threads 등** — 등록된 커스텀 URL 스킴으로 앱 실행을 시도하고, "외부 브라우저로 열기" 안내를 함께 보여줍니다

링크 서버에서 처리되므로 SDK 코드를 추가할 필요가 없습니다.

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

## 주의사항

### 디퍼드 딥링크 — Android Auto Backup

Android 6.0 이상에서는 앱 데이터가 Google Drive에 자동 백업됩니다. SDK가 첫 실행 여부를 `SharedPreferences`에 저장하기 때문에, **앱 삭제 후 재설치 시 백업이 복원되어 디퍼드 딥링크 매칭이 발동하지 않을 수 있습니다**.

재설치 후에도 디퍼드 딥링크가 필요하다면 아래 백업 제외 규칙을 추가하세요.

**`res/xml/data_extraction_rules.xml`** (Android 12+):
```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="vialink_sdk"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="vialink_sdk"/>
    </device-transfer>
</data-extraction-rules>
```

**`res/xml/backup_rules.xml`** (Android 11 이하):
```xml
<full-backup-content>
    <exclude domain="sharedpref" path="vialink_sdk.xml"/>
</full-backup-content>
```

`AndroidManifest.xml`에서 두 파일을 연결합니다:
```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    ...>
```

## 샘플 프로젝트

`sample/` 디렉토리에서 실행 가능한 샘플 앱을 확인하세요.

## 문서

- [SDK 가이드](https://docs.vialink.app/#sdk-android-install)

## 라이선스

전용 라이선스 — © 2026 Aresjoy Inc. All rights reserved.
사용 조건은 [ViaLink 이용약관](https://vialink.app/terms?lang=ko)을 따릅니다. 자세한 내용은 [LICENSE](LICENSE)를 참고하세요.

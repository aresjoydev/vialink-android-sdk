# ViaLink Android SDK

**English** | [한국어](README.ko.md)

Android SDK for the ViaLink deep link infrastructure service.

## Features

- **Deep link routing** — automatic handling of App Links / Custom Schemes
- **Deferred deep linking** — fingerprint-based matching on the first launch after install
- **Event tracking** — batched delivery of custom events
- **Payment attribution** — records payment attempts and automatically attaches `link_id`
- **Link creation** — generate deep links from within the app (static/dynamic)

## Requirements

- Android API 24 (7.0)+
- Kotlin 1.9+

## Installation

### 1) Register the repository (settings.gradle.kts)

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://aresjoydev.github.io/vialink-android-sdk") }
    }
}
```

### 2) Add the dependency (app/build.gradle.kts)

```kotlin
dependencies {
    implementation("com.vialink:sdk:<version>")
}
```

> Check the latest version from the release tags on the [GitHub repository](https://github.com/aresjoydev/vialink-android-sdk), or at `https://aresjoydev.github.io/vialink-android-sdk/com/vialink/sdk/maven-metadata.xml`.

## Usage

### 1. Initialization

```kotlin
// Initialize in Application.onCreate
ViaLinkSDK.init(this, "YOUR_API_KEY")
```

### 2. Deep link callbacks

```kotlin
// Receive App Links / custom schemes
ViaLinkSDK.onDeepLink { data ->
    Log.d("ViaLink", "path: ${data.path}")
    Log.d("ViaLink", "params: ${data.params}")
}

// Deferred deep link (matched after the first install)
ViaLinkSDK.onDeferredDeepLink { data, error ->
    if (error != null) {
        Log.e("ViaLink", "match failed: ${error.message}")
        return@onDeferredDeepLink
    }
    if (data != null) {
        Log.d("ViaLink", "deferred: ${data.path}")
    } else {
        Log.d("ViaLink", "no match (organic)")
    }
}
```

**Important**: You must call `handleIntent` in your Activity to process intents.

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
// Synchronous (returns the cached value immediately)
val deepLink = ViaLinkSDK.getDeepLinkData()
val deferred = ViaLinkSDK.getDeferredLinkData()

// Asynchronous (waits until the result arrives, in a coroutine context)
lifecycleScope.launch {
    val deepLinkAsync = ViaLinkSDK.awaitDeepLinkData()    // 3-second timeout
    val deferredAsync = ViaLinkSDK.awaitDeferredLinkData() // waits until the result
}
```

### 4. Event tracking

```kotlin
ViaLinkSDK.track("purchase", mapOf(
    "product_id" to "12345",
    "revenue" to 29900,
    "currency" to "KRW"
))
```

### 5. Payment tracking

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

### 6. Link creation

```kotlin
lifecycleScope.launch {
    val result = ViaLinkSDK.createLink(
        path = "/product/12345",
        data = mapOf("promo_code" to "FRIEND_SHARE"),
        campaign = "referral",
        linkType = "dynamic" // when click tracking is needed
    )
    result.onSuccess { url -> Log.d("ViaLink", "created link: $url") }
    result.onFailure { err -> Log.e("ViaLink", "creation failed: ${err.message}") }
}
```

## Notes

### Deferred deep link — Android Auto Backup

On Android 6.0 and above, app data is automatically backed up to Google Drive. Because the SDK stores the first-launch flag in `SharedPreferences`, **the backup may be restored on reinstall after uninstall, which can prevent deferred deep link matching from firing**.

If you need deferred deep linking to work even after reinstall, add the backup exclusion rules below.

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

**`res/xml/backup_rules.xml`** (Android 11 and below):
```xml
<full-backup-content>
    <exclude domain="sharedpref" path="vialink_sdk.xml"/>
</full-backup-content>
```

Wire both files in `AndroidManifest.xml`:
```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    ...>
```

## Sample project

See the runnable sample app in the `sample/` directory.

## Documentation

- [SDK Guide](https://docs.vialink.app/sdk/android)

## License

MIT License — Aresjoy Inc.

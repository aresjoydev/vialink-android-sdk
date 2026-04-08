package com.vialink.sdk

import android.app.Application
import android.content.Context
import android.content.Intent
import com.vialink.sdk.model.DeepLinkData
import com.vialink.sdk.model.DeviceInfo
import com.vialink.sdk.storage.SharedPrefsStorage
import kotlinx.coroutines.*

/// ViaLink Android SDK
///
/// 딥링크 라우팅, 디퍼드 딥링킹, 이벤트 추적을 제공합니다.
///
/// ```kotlin
/// // 초기화 (Application.onCreate)
/// ViaLinkSDK.init(this, "YOUR_API_KEY")
///
/// // 딥링크 콜백
/// ViaLinkSDK.onDeepLink { data ->
///     navigate(data.path, data.params)
/// }
///
/// // Activity에서 App Link 처리
/// ViaLinkSDK.handleIntent(intent)
/// ```
object ViaLinkSDK {

    /// ViaLink API 서버 주소 (빌드 시 고정, 외부 변경 불가)
    private const val API_BASE_URL = "https://vialink.app"

    private lateinit var appContext: Context
    private lateinit var networkClient: NetworkClient
    private lateinit var eventTracker: EventTracker
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var storage: SharedPrefsStorage

    private var deepLinkHandler: ((DeepLinkData) -> Unit)? = null
    private var deferredHandler: ((DeepLinkData) -> Unit)? = null
    private var isConfigured = false
    // App Link에서 전달받은 fp 파라미터 (디퍼드 딥링킹용)
    private var pendingFp: String? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /// SDK 초기화 - Application.onCreate()에서 호출
    /// @param context Application Context
    /// @param apiKey 대시보드에서 발급받은 API Key
    fun init(context: Context, apiKey: String) {
        if (isConfigured) {
            ViaLinkLog.info("이미 초기화되었습니다")
            return
        }

        appContext = context.applicationContext
        networkClient = NetworkClient(API_BASE_URL, apiKey)
        deviceInfo = DeviceInfoCollector.collect(appContext)
        storage = SharedPrefsStorage(appContext)
        eventTracker = EventTracker(networkClient, storage)

        isConfigured = true

        // 배치 전송 타이머 시작 (30초)
        val app = appContext as? Application
        if (app != null) {
            eventTracker.startBatchTimer(app)
        }

        ViaLinkLog.info("SDK 초기화 완료 (apiKey: ${apiKey.take(8)}...)")

        // 첫 실행 체크 -> 디퍼드 딥링크 매칭
        ViaLinkLog.info("hasLaunched=${storage.hasLaunched}")
        if (!storage.hasLaunched) {
            storage.hasLaunched = true
            ViaLinkLog.info("첫 실행 감지 → 디퍼드 매칭 시작")
            scope.launch {
                try {
                    attemptDeferredMatch()
                } catch (e: Exception) {
                    ViaLinkLog.error("디퍼드 매칭 코루틴 실패", e)
                }
            }
            track("app.install")
        } else {
            ViaLinkLog.info("재실행 (hasLaunched=true)")
            track("app.open")
        }
    }

    /// 딥링크 수신 콜백 등록
    /// App Link로 앱이 실행되었을 때 호출됩니다.
    fun onDeepLink(handler: (DeepLinkData) -> Unit) {
        deepLinkHandler = handler
    }

    /// 디퍼드 딥링크 콜백 등록
    /// 앱 첫 설치 후 핑거프린트 매칭이 성공했을 때 호출됩니다.
    fun onDeferredDeepLink(handler: (DeepLinkData) -> Unit) {
        deferredHandler = handler
    }

    /// Activity에서 App Link Intent 처리
    ///
    /// ```kotlin
    /// override fun onCreate(savedInstanceState: Bundle?) {
    ///     super.onCreate(savedInstanceState)
    ///     ViaLinkSDK.handleIntent(intent)
    /// }
    /// ```
    fun handleIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false

        // fp 파라미터 저장 (디퍼드 딥링킹용 — 설치 후 첫 실행 시 사용)
        val fp = uri.getQueryParameter("fp")
        if (fp != null) {
            pendingFp = fp
            storage.saveFp(fp)
            ViaLinkLog.info("fp 파라미터 저장: ${fp.take(16)}...")
        }

        val handler = DeepLinkHandler()
        val shortCode = handler.parseAppLink(uri) ?: return false

        ViaLinkLog.info("App Link 수신: $shortCode")

        if (!isConfigured) {
            ViaLinkLog.error("SDK가 초기화되지 않았습니다")
            return false
        }

        scope.launch {
            val data = handler.fetchLinkData(shortCode, networkClient)
            if (data != null) {
                withContext(Dispatchers.Main) {
                    deepLinkHandler?.invoke(data)
                }
                track("app.deeplink", data = mapOf("short_code" to shortCode))
            }
        }

        return true
    }

    /// 커스텀 이벤트 추적
    ///
    /// ```kotlin
    /// ViaLinkSDK.track("purchase", data = mapOf("product_id" to "123", "revenue" to 29900))
    /// ```
    fun track(eventName: String, data: Map<String, Any>? = null) {
        if (!isConfigured) {
            ViaLinkLog.error("SDK가 초기화되지 않았습니다")
            return
        }
        eventTracker.track(eventName, data = data)
    }

    /// 앱 내에서 딥링크 생성
    ///
    /// ```kotlin
    /// val result = ViaLinkSDK.createLink(
    ///     path = "/product/123",
    ///     data = mapOf("promo_code" to "FRIEND"),
    ///     campaign = "referral"
    /// )
    /// ```
    suspend fun createLink(
        path: String,
        data: Map<String, Any>? = null,
        campaign: String? = null
    ): Result<String> {
        if (!isConfigured) {
            return Result.failure(IllegalStateException("SDK가 초기화되지 않았습니다"))
        }
        return networkClient.createLink(path, data, campaign)
    }

    /// 디퍼드 딥링크 매칭 시도
    private suspend fun attemptDeferredMatch() {
        // 저장된 fp 파라미터가 있으면 함께 전달 (직접 매칭)
        val fp = pendingFp ?: storage.loadFp()
        val matcher = DeferredDeepLinkMatcher(networkClient, deviceInfo)
        val data = matcher.match(fp)
        if (data != null) {
            ViaLinkLog.info("디퍼드 딥링크 매칭 성공: ${data.path}")
            withContext(Dispatchers.Main) {
                deferredHandler?.invoke(data)
            }
        }
    }
}

package com.vialink.sample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.vialink.sample.ui.theme.ViaLinkSampleTheme
import com.vialink.sdk.ViaLinkSDK
import com.vialink.sdk.model.PaymentInitiatedArgs
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App Link Intent 처리 (항상 호출)
        ViaLinkSDK.handleIntent(intent)

        setContent {
            ViaLinkSampleTheme {
            ViaLinkSampleScreen(
                    onTrackEvent = { eventName, data ->
                        ViaLinkSDK.track(eventName, data)
                        Log.d("ViaLink", "이벤트 전송: $eventName")
                    },
                    onCreateLink = { path, campaign, linkType ->
                        lifecycleScope.launch {
                            val result = ViaLinkSDK.createLink(
                                path = path,
                                campaign = campaign,
                                linkType = linkType
                            )
                            result.onSuccess { shortUrl ->
                                Log.d("ViaLink", "[$linkType] 링크 생성 성공: $shortUrl")
                                DeepLinkState.postResult("[$linkType] 링크 생성 성공", shortUrl, copyableText = shortUrl)
                            }
                            result.onFailure { err ->
                                Log.e("ViaLink", "[$linkType] 링크 생성 실패: ${err.message}")
                                DeepLinkState.postResult("[$linkType] 링크 생성 실패", err.message ?: "알 수 없는 오류")
                            }
                        }
                    },
                    onPaymentInitiated = {
                        lifecycleScope.launch {
                            try {
                                val result = ViaLinkSDK.trackPayment(
                                    PaymentInitiatedArgs(
                                        orderId = "ORD-2026-0001",
                                        amount = 19900.0,
                                        currency = "KRW",
                                        paymentMethod = "card",
                                        metadata = mapOf("productId" to "prod-001"),
                                    )
                                )
                                Log.d("ViaLink", "payment_event_id=${result.paymentEventId}")
                                DeepLinkState.postResult(
                                    "결제 시도 완료",
                                    "success: ${result.success}\npaymentEventId: ${result.paymentEventId}"
                                )
                            } catch (e: IllegalArgumentException) {
                                Log.e("ViaLink", "입력 검증 실패: ${e.message}")
                                DeepLinkState.postResult("결제 시도 실패", "입력 검증 실패: ${e.message}")
                            } catch (e: Exception) {
                                Log.e("ViaLink", "네트워크 오류: ${e.message}")
                                DeepLinkState.postResult("결제 시도 실패", "네트워크 오류: ${e.message}")
                            }
                        }
                    },
                    onGetDeferredLinkData = {
                        val data = ViaLinkSDK.getDeferredLinkData()
                        if (data != null) {
                            DeepLinkState.postResult(
                                "디퍼드 데이터 (Sync Pull)",
                                "경로: ${data.path}\n파라미터: ${data.params}\nshortCode: ${data.shortCode ?: "없음"}\nlinkId: ${data.linkId ?: "없음"}"
                            )
                        } else {
                            DeepLinkState.postResult("디퍼드 데이터 (Sync Pull)", "매칭된 데이터 없음 (또는 대기 중)")
                        }
                    },
                    onAwaitDeferredLinkData = {
                        lifecycleScope.launch {
                            try {
                                val data = ViaLinkSDK.awaitDeferredLinkData()
                                if (data != null) {
                                    DeepLinkState.postResult(
                                        "디퍼드 데이터 (Await)",
                                        "경로: ${data.path}\n파라미터: ${data.params}\nshortCode: ${data.shortCode ?: "없음"}\nlinkId: ${data.linkId ?: "없음"}"
                                    )
                                } else {
                                    DeepLinkState.postResult("디퍼드 데이터 (Await)", "매칭된 데이터 없음")
                                }
                            } catch (e: Exception) {
                                DeepLinkState.postResult("디퍼드 데이터 실패", e.message ?: "오류")
                            }
                        }
                    },
                    onGetDeepLinkData = {
                        val data = ViaLinkSDK.getDeepLinkData()
                        if (data != null) {
                            DeepLinkState.postResult(
                                "딥링크 데이터 (Sync Pull)",
                                "경로: ${data.path}\n파라미터: ${data.params}\nshortCode: ${data.shortCode ?: "없음"}\nlinkId: ${data.linkId ?: "없음"}"
                            )
                        } else {
                            DeepLinkState.postResult("딥링크 데이터 (Sync Pull)", "수신된 딥링크 없음")
                        }
                    },
                    onAwaitDeepLinkData = {
                        lifecycleScope.launch {
                            try {
                                val data = ViaLinkSDK.awaitDeepLinkData()
                                if (data != null) {
                                    DeepLinkState.postResult(
                                        "딥링크 데이터 (Await)",
                                        "경로: ${data.path}\n파라미터: ${data.params}\nshortCode: ${data.shortCode ?: "없음"}\nlinkId: ${data.linkId ?: "없음"}"
                                    )
                                } else {
                                    DeepLinkState.postResult("딥링크 데이터 (Await)", "수신 없음")
                                }
                            } catch (e: Exception) {
                                DeepLinkState.postResult("딥링크 데이터 대기 실패", e.message ?: "오류")
                            }
                        }
                    },
                )
            }
        }
    }

    // 앱이 이미 실행 중일 때 새 Intent를 받으면 호출
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ViaLinkSDK.handleIntent(intent)

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViaLinkSampleScreen(
    onTrackEvent: (String, Map<String, Any>?) -> Unit,
    onCreateLink: (String, String, String) -> Unit,
    onPaymentInitiated: () -> Unit,
    onGetDeferredLinkData: () -> Unit,
    onAwaitDeferredLinkData: () -> Unit,
    onGetDeepLinkData: () -> Unit,
    onAwaitDeepLinkData: () -> Unit,
) {
    val context = LocalContext.current

    // 딥링크/디퍼드/링크생성/결제 결과를 AlertDialog로 표시
    val deepLinkResult by DeepLinkState.result.collectAsState()

    if (deepLinkResult != null) {
        AlertDialog(
            onDismissRequest = { DeepLinkState.consume() },
            title = { Text(deepLinkResult!!.title) },
            text = { Text(deepLinkResult!!.message) },
            confirmButton = {
                TextButton(onClick = { DeepLinkState.consume() }) {
                    Text("확인")
                }
            },
            dismissButton = if (deepLinkResult!!.copyableText != null) {
                {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ViaLink URL", deepLinkResult!!.copyableText))
                        Toast.makeText(context, "📋 링크가 복사되었습니다", Toast.LENGTH_SHORT).show()
                        DeepLinkState.consume()
                    }) {
                        Text("복사하기")
                    }
                }
            } else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ViaLinkSample") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 이벤트 추적 섹션 ──
            SectionTitle("이벤트 추적")

            Button(
                onClick = {
                    onTrackEvent("signup", null)
                    Toast.makeText(context, "✅ 회원가입 이벤트 전송 완료", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("회원가입 이벤트 전송")
            }

            Button(
                onClick = {
                    onTrackEvent("purchase", mapOf(
                        "product_id" to "12345",
                        "revenue" to 29900,
                        "currency" to "KRW"
                    ))
                    Toast.makeText(context, "✅ 구매 이벤트 전송 완료", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("구매 이벤트 전송")
            }

            Button(
                onClick = {
                    onTrackEvent("add_to_cart", mapOf(
                        "product_id" to "12345"
                    ))
                    Toast.makeText(context, "✅ 장바구니 추가 이벤트 전송 완료", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("장바구니 추가 이벤트 전송")
            }

            HorizontalDivider()

            // ── 링크 생성 섹션 ──
            SectionTitle("링크 생성")

            Button(
                onClick = {
                    onCreateLink("/product/12345", "referral", "static")
                    Toast.makeText(context, "🔗 Static 링크 생성 요청 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Static 링크 생성 (추적 없음)")
            }

            Button(
                onClick = {
                    onCreateLink("/product/12345", "referral", "dynamic")
                    Toast.makeText(context, "🔗 Dynamic 링크 생성 요청 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dynamic 링크 생성 (클릭 추적)")
            }

            HorizontalDivider()

            // ── 결제 추적 섹션 ──
            SectionTitle("결제 추적")

            Button(
                onClick = {
                    onPaymentInitiated()
                    Toast.makeText(context, "💳 결제 시도 요청 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("결제 시도 (initiated)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            // ── Pull API 섹션 ──
            SectionTitle("Pull API")

            Button(
                onClick = {
                    onGetDeferredLinkData()
                    Toast.makeText(context, "🔍 디퍼드 데이터 요청 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("디퍼드 딥링크 데이터 즉시 조회 (Sync)")
            }

            Button(
                onClick = {
                    onAwaitDeferredLinkData()
                    Toast.makeText(context, "⏳ 디퍼드 데이터 대기 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("디퍼드 딥링크 데이터 대기 (Await)")
            }

            Button(
                onClick = {
                    onGetDeepLinkData()
                    Toast.makeText(context, "🔍 딥링크 데이터 요청 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("딥링크 데이터 즉시 조회 (Sync)")
            }

            Button(
                onClick = {
                    onAwaitDeepLinkData()
                    Toast.makeText(context, "⏳ 딥링크 데이터 5초 대기 중...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("딥링크 데이터 수신 대기 (Await)")
            }

            // ── 안내 텍스트 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SDK 정보",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• SDK 버전: ${ViaLinkSDK.SDK_VERSION}\n" +
                                "• 딥링크/디퍼드 결과는 AlertDialog로 표시\n" +
                                "• Logcat에서 'ViaLink' 태그로 상세 확인\n" +
                                "• 이벤트는 30초마다 배치 전송",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
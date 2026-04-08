package com.vialink.sdk.model

/// 딥링크 데이터 모델
data class DeepLinkData(
    val path: String,
    val params: Map<String, String> = emptyMap(),
    val shortCode: String? = null
)

package com.vialink.sdk.model

/// 디바이스 정보 모델
data class DeviceInfo(
    val os: String,
    val osVersion: String,
    val model: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val language: String,
    val country: String,
    val carrier: String?
) {
    fun toDictionary(): Map<String, Any?> = mapOf(
        "os" to os,
        "os_version" to osVersion,
        "device_model" to model,
        "screen_width" to screenWidth,
        "screen_height" to screenHeight,
        "language" to language,
        "country" to country,
        "carrier" to carrier
    )
}

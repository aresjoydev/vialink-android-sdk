# ViaLink SDK ProGuard rules
# 공개 API 클래스 유지
-keep class com.vialink.sdk.ViaLinkSDK { *; }
-keep class com.vialink.sdk.model.DeepLinkData { *; }
-keep class com.vialink.sdk.model.DeviceInfo { *; }
-keep class com.vialink.sdk.model.EventPayload { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

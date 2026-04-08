package com.vialink.sdk

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.vialink.sdk.model.DeviceInfo

/// 디바이스 정보 수집
internal object DeviceInfoCollector {

    fun collect(context: Context): DeviceInfo {
        val dm = context.resources.displayMetrics
        val locale = context.resources.configuration.locales[0]

        return DeviceInfo(
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            screenWidth = dm.widthPixels,
            screenHeight = dm.heightPixels,
            language = "${locale.language}-${locale.country}",
            country = locale.country,
            carrier = getCarrier(context)
        )
    }

    /// 통신사 정보
    private fun getCarrier(context: Context): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.networkOperatorName?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

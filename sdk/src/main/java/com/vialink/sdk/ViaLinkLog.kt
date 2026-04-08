package com.vialink.sdk

import android.util.Log

/// ViaLink 내부 로거
internal object ViaLinkLog {
    private const val TAG = "ViaLink"

    fun info(message: String) {
        Log.i(TAG, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}

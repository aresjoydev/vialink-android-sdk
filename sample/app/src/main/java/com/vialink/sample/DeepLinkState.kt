package com.vialink.sample

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/// 딥링크/디퍼드 딥링크 결과를 MyApplication → MainActivity UI로 전달하는 공유 상태
object DeepLinkState {
    data class Result(
        val title: String,
        val message: String,
        val copyableText: String? = null,
    )

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result

    fun postResult(title: String, message: String, copyableText: String? = null) {
        _result.value = Result(title, message, copyableText)
    }

    fun consume() {
        _result.value = null
    }
}

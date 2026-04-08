package com.vialink.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.vialink.sdk.model.EventPayload
import com.vialink.sdk.storage.SharedPrefsStorage
import kotlinx.coroutines.*

/// 이벤트 큐 + 배치 전송 (30초 간격)
internal class EventTracker(
    private val client: NetworkClient,
    private val storage: SharedPrefsStorage
) {
    private val queue = mutableListOf<EventPayload>()
    private val lock = Any()
    private val maxQueueSize = 100
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // 미전송 이벤트 복원
        queue.addAll(storage.loadPendingEvents())
    }

    /// 이벤트 추가
    fun track(eventName: String, linkId: Int? = null, data: Map<String, Any>? = null) {
        val event = EventPayload(
            linkId = linkId,
            eventName = eventName,
            eventData = data
        )

        synchronized(lock) {
            queue.add(event)
        }

        if (queue.size >= maxQueueSize) {
            scope.launch { flush() }
        }
    }

    /// 배치 전송 타이머 시작 (30초)
    fun startBatchTimer(application: Application) {
        timerJob = scope.launch {
            while (isActive) {
                delay(30_000)
                flush()
            }
        }

        // 앱 백그라운드 진입 시 즉시 전송 + 미전송분 저장
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var activityCount = 0

            override fun onActivityStarted(activity: Activity) { activityCount++ }
            override fun onActivityStopped(activity: Activity) {
                activityCount--
                if (activityCount == 0) {
                    // 앱이 백그라운드로 전환
                    scope.launch {
                        flush()
                        synchronized(lock) {
                            storage.savePendingEvents(queue)
                        }
                    }
                }
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }

    /// 큐 전송
    suspend fun flush() {
        val events: List<EventPayload>
        synchronized(lock) {
            events = queue.toList()
            queue.clear()
        }

        if (events.isEmpty()) return

        try {
            if (events.size == 1) {
                val event = events.first()
                client.post("/v1/events", event.toDictionary())
            } else {
                val body = mapOf<String, Any?>(
                    "events" to events.map { it.toBatchItem() }
                )
                client.post("/v1/events/batch", body)
            }
            storage.clearPendingEvents()
        } catch (e: Exception) {
            // 실패 시 큐에 복원
            synchronized(lock) {
                queue.addAll(0, events)
                storage.savePendingEvents(queue)
            }
            ViaLinkLog.error("이벤트 전송 실패", e)
        }
    }

    fun destroy() {
        timerJob?.cancel()
        scope.cancel()
    }
}

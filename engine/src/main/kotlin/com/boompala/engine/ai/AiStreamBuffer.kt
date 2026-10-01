package com.boompala.engine.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 针对 Wear OS 手表低功耗小屏优化的流式文本缓冲聚合扩展。
 *
 * 将高频连续到达的单个 token (如 "这" "是" "一" "个") 在时间窗口内（默认 90ms）
 * 聚合为一个 TextDelta ("这是一个") 发射，大幅降低下游 UI 页面重组频率与 CPU 唤醒次数，
 * 同时在流暂停或结束时立即排空缓冲区，兼顾打字机流畅视觉体验与手表续航。
 */
fun Flow<AiStreamEvent>.bufferTextDeltas(windowMs: Long = 90L): Flow<AiStreamEvent> = channelFlow {
    val deltaBuffer = StringBuilder()
    val mutex = Mutex()
    var lastFlushTime = System.currentTimeMillis()

    val tickerJob = launch {
        while (isActive) {
            delay((windowMs / 2).coerceAtLeast(10L))
            mutex.withLock {
                if (deltaBuffer.isNotEmpty() && (System.currentTimeMillis() - lastFlushTime >= windowMs)) {
                    val combined = deltaBuffer.toString()
                    deltaBuffer.clear()
                    lastFlushTime = System.currentTimeMillis()
                    send(AiStreamEvent.TextDelta(combined))
                }
            }
        }
    }

    try {
        collect { event ->
            when (event) {
                is AiStreamEvent.TextDelta -> {
                    mutex.withLock {
                        deltaBuffer.append(event.text)
                        val now = System.currentTimeMillis()
                        if (now - lastFlushTime >= windowMs) {
                            val combined = deltaBuffer.toString()
                            deltaBuffer.clear()
                            lastFlushTime = now
                            send(AiStreamEvent.TextDelta(combined))
                        }
                    }
                }
                is AiStreamEvent.Completed -> {
                    mutex.withLock {
                        if (deltaBuffer.isNotEmpty()) {
                            val remaining = deltaBuffer.toString()
                            deltaBuffer.clear()
                            send(AiStreamEvent.TextDelta(remaining))
                        }
                    }
                    send(event)
                }
                is AiStreamEvent.Error -> {
                    mutex.withLock {
                        if (deltaBuffer.isNotEmpty()) {
                            val remaining = deltaBuffer.toString()
                            deltaBuffer.clear()
                            send(AiStreamEvent.TextDelta(remaining))
                        }
                    }
                    send(event)
                }
            }
        }
    } finally {
        tickerJob.cancel()
        mutex.withLock {
            if (deltaBuffer.isNotEmpty()) {
                val remaining = deltaBuffer.toString()
                deltaBuffer.clear()
                send(AiStreamEvent.TextDelta(remaining))
            }
        }
    }
}
